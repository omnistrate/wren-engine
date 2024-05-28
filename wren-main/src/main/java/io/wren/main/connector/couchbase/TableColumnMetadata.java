package io.wren.main.connector.couchbase;

import com.google.common.collect.ImmutableList;
import io.wren.base.Column;

import java.util.List;

/**
 * 'SELECT \
 * table_catalog, table_schema, table_name, column_name, ordinal_position, is_nullable, data_type\
 * FROM INFORMATION_SCHEMA.COLUMNS;';
 */
public record TableColumnMetadata(String tableCatalog, String tableSchema, String tableName, String columnName,
                                  int ordinalPosition, boolean isNullable, String dataType) {
    public static List<Column> getColumns()
    {
        ImmutableList.Builder<Column> builder = ImmutableList.builder();
        builder.add(new Column("table_catalog", "String"));
        builder.add(new Column("table_schema", "String"));
        builder.add(new Column("table_name", "String"));
        builder.add(new Column("column_name", "String"));
        builder.add(new Column("ordinal_position", "int"));
        builder.add(new Column("is_nullable", "boolean"));
        builder.add(new Column("data_type", "String"));

        return builder.build();
    }
}
