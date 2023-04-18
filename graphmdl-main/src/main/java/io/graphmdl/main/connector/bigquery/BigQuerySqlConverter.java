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

package io.graphmdl.main.connector.bigquery;

import com.google.common.collect.ImmutableList;
import io.graphmdl.base.SessionContext;
import io.graphmdl.main.calcite.QueryProcessor;
import io.graphmdl.main.metadata.Metadata;
import io.graphmdl.main.sql.SqlConverter;
import io.graphmdl.main.sql.SqlRewrite;
import io.graphmdl.main.sql.bigquery.RemoveCatalogSchemaColumnPrefix;
import io.graphmdl.main.sql.bigquery.RemoveColumnAliasInAliasRelation;
import io.graphmdl.main.sql.bigquery.ReplaceColumnAliasInUnnest;
import io.graphmdl.main.sql.bigquery.TransformCorrelatedJoinToJoin;
import io.trino.sql.tree.Node;
import org.intellij.lang.annotations.Language;

import javax.inject.Inject;

import java.util.List;

import static io.graphmdl.sqlrewrite.Utils.parseSql;
import static io.trino.sql.SqlFormatter.formatSql;
import static java.util.Objects.requireNonNull;

public class BigQuerySqlConverter
        implements SqlConverter
{
    private final Metadata metadata;

    @Inject
    public BigQuerySqlConverter(Metadata metadata)
    {
        this.metadata = requireNonNull(metadata, "metadata is null");
    }

    @Override
    public String convert(@Language("sql") String sql, SessionContext sessionContext)
    {
        QueryProcessor processor = QueryProcessor.of(metadata);
        Node rewrittenNode = parseSql(sql);

        List<SqlRewrite> sqlRewrites = ImmutableList.of(
                // bigquery doesn't support column name with catalog.schema.table prefix or schema.table prefix
                RemoveCatalogSchemaColumnPrefix.INSTANCE,
                // bigquery doesn't support column alias in alias relation
                RemoveColumnAliasInAliasRelation.INSTANCE,
                // bigquery doesn't support column alias in unnest alias relation
                ReplaceColumnAliasInUnnest.INSTANCE,
                // bigquery doesn't support correlated join in where clause
                TransformCorrelatedJoinToJoin.INSTANCE);

        for (SqlRewrite rewrite : sqlRewrites) {
            rewrittenNode = rewrite.rewrite(rewrittenNode);
        }

        return processor.convert(formatSql(rewrittenNode));
    }
}
