/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.wren.main.connector.couchbase;

import com.google.common.collect.ImmutableList;
import io.airlift.log.Logger;
import io.wren.base.Column;
import io.wren.base.Parameter;
import io.wren.base.WrenException;
import io.wren.base.config.CouchbaseConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import static io.wren.base.metadata.StandardErrorCode.GENERIC_USER_ERROR;
import static io.wren.main.connector.couchbase.CouchbaseType.toPGType;
import static java.util.Objects.requireNonNull;

public class CouchbaseClient
{
    private static final Logger LOG = Logger.get(CouchbaseClient.class);

    private final CouchbaseConfig config;

    public CouchbaseClient(CouchbaseConfig config)
    {
        this.config = requireNonNull(config, "config is null");
    }

    public Connection createConnection()
            throws SQLException
    {
        cdata.jdbc.couchbase.CouchbaseDriver.register();

        Properties props = new Properties();
        props.setProperty("User", config.getUser());
        props.setProperty("Password", config.getPassword());
        props.setProperty("Server", config.getPassword());
        config.getUseSSL().ifPresent(useSSL -> props.setProperty("UseSSL", String.valueOf(useSSL)));
        config.getN1QLPort().ifPresent(n1qlPort -> props.setProperty("N1QLPort", n1qlPort));

        Connection connection = DriverManager.getConnection(config.getJdbcUrl(), props);

        Statement statement = connection.createStatement();
        statement.execute("SELECT 1");

        return connection;
    }

    // Retrieve all table metadata in the format

    /**
     *       'SELECT \
     *       table_catalog, table_schema, table_name, column_name, ordinal_position, is_nullable, data_type\
     *       FROM INFORMATION_SCHEMA.COLUMNS;';
     */
    public List<TableColumnMetadata> getSchema()
    {
        try (Connection connection = createConnection()) {
            // Reset schema
            connection.createStatement().execute("RESET SCHEMA CACHE");
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet tables = metaData.getTables(null, null, null, new String[] {"TABLE"})) {
                ImmutableList.Builder<TableColumnMetadata> builder = ImmutableList.builder();
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    String tableSchema = tables.getString("TABLE_SCHEM");
                    String tableCatalog = tables.getString("TABLE_CAT");
                    try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
                        while (columns.next()) {
                            String columnName = columns.getString("COLUMN_NAME");
                            int ordinalPosition = columns.getInt("ORDINAL_POSITION");
                            boolean isNullable = columns.getBoolean("IS_NULLABLE");
                            String dataType = columns.getString("DATA_TYPE");
                            builder.add(new TableColumnMetadata(
                                    "\"" + tableCatalog + "\"",
                                    "\"" + tableSchema + "\"",
                                    "\"" + tableName + "\"",
                                    columnName,
                                    ordinalPosition,
                                    isNullable,
                                    dataType));
                        }
                    }
                }
                return builder.build();
            }
        }
        catch (SQLException e) {
            LOG.error(e, "Error executing getSchema");
            throw new WrenException(GENERIC_USER_ERROR, e);
        }
    }

    public void execute(String sql)
    {
        try (Connection connection = createConnection()) {
            connection.createStatement().execute(sql);
        }
        catch (Exception e) {
            LOG.error(e, "Error executing DDL");
            throw new WrenException(GENERIC_USER_ERROR, e);
        }
    }

    public CouchbaseRecordIterator query(String sql, List<Parameter> parameters)
    {
        try {
            return CouchbaseRecordIterator.of(createConnection(), sql, parameters);
        }
        catch (Exception e) {
            LOG.error(e, "Error executing query");
            throw new WrenException(GENERIC_USER_ERROR, e);
        }
    }

    public List<Column> describe(String sql, List<Parameter> parameters)
    {
        try (Connection connection = createConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i).getValue());
            }

            return buildColumns(statement.getMetaData());
        }
        catch (SQLException e) {
            LOG.error(e, "Error executing describe");
            throw new WrenException(GENERIC_USER_ERROR, e);
        }
    }

    public static List<Column> buildColumns(ResultSetMetaData metaData)
            throws SQLException
    {
        ImmutableList.Builder<Column> builder = ImmutableList.builder();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            builder.add(new Column(metaData.getColumnName(i), toPGType(metaData.getColumnType(i))));
        }
        return builder.build();
    }
}
