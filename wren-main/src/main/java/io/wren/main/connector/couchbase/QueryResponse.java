package io.wren.main.connector.couchbase;

import io.wren.base.Column;

import java.util.List;

public record QueryResponse<T>(List<T> data, List<Column> columns){
}
