package io.wren.main.connector.couchbase;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 'SELECT \
 * table_catalog, table_schema, table_name, column_name, ordinal_position, is_nullable, data_type\
 * FROM INFORMATION_SCHEMA.COLUMNS;';
 */
public record TableColumnMetadata(
        @JsonProperty("table_catalog") String tableCatalog,
        @JsonProperty("table_schema") String tableSchema,
        @JsonProperty("table_name") String tableName,
        @JsonProperty("column_name") String columnName,
        @JsonProperty("ordinal_position") int ordinalPosition,
        @JsonProperty("is_nullable") boolean isNullable,
        @JsonProperty("data_type") String dataType)
{
}
