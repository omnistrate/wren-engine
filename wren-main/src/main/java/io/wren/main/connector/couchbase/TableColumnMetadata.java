package io.wren.main.connector.couchbase;

/**
 * 'SELECT \
 * table_catalog, table_schema, table_name, column_name, ordinal_position, is_nullable, data_type\
 * FROM INFORMATION_SCHEMA.COLUMNS;';
 */
public record TableColumnMetadata(String tableCatalog, String tableSchema, String tableName, String columnName,
                                  int ordinalPosition, boolean isNullable, String dataType) {
}
