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

package io.graphmdl.main.calcite;

import io.airlift.log.Logger;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlAlienSystemTypeNameSpec;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlBasicTypeNameSpec;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlCollectionTypeNameSpec;
import org.apache.calcite.sql.SqlDataTypeSpec;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlTypeNameSpec;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.dialect.BigQuerySqlDialect;
import org.apache.calcite.sql.fun.SqlCase;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.BasicSqlType;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;

public class BigQueryGraphMDLSqlDialect
        extends BigQuerySqlDialect
{
    private static final Logger LOG = Logger.get(BigQueryGraphMDLSqlDialect.class);

    /**
     * Creates a BigQuerySqlDialect.
     *
     * @param context
     */
    public BigQueryGraphMDLSqlDialect(Context context)
    {
        super(context);
    }

    @Override
    public void quoteStringLiteralUnicode(StringBuilder buf, String val)
    {
        // refer to https://blog.csdn.net/weixin_39133753/article/details/115470036
        buf.append(literalQuoteString);
        buf.append(val.replace(literalEndQuoteString, literalEscapedQuote));
        buf.append(literalQuoteString);
    }

    @Override
    public SqlNode getCastSpec(final RelDataType type)
    {
        if (type instanceof BasicSqlType) {
            final SqlTypeName typeName = type.getSqlTypeName();
            switch (typeName) {
                // BigQuery only supports INT64 for integer types.
                case TINYINT:
                case SMALLINT:
                case INTEGER:
                case BIGINT:
                    return createSqlDataTypeSpecByName("INT64", typeName);
                // BigQuery only supports FLOAT64(aka. Double) for floating point types.
                case FLOAT:
                case DOUBLE:
                    return createSqlDataTypeSpecByName("FLOAT64", typeName);
                case DECIMAL:
                    if (type.getPrecision() <= 38) {
                        return createSqlDataTypeSpecByName("NUMERIC", typeName);
                    }
                    return createSqlDataTypeSpecByName("BIGNUMERIC", typeName);
                case BOOLEAN:
                    return createSqlDataTypeSpecByName("BOOL", typeName);
                case CHAR:
                case VARCHAR:
                    return createSqlDataTypeSpecByName("STRING", typeName);
                case BINARY:
                case VARBINARY:
                    return createSqlDataTypeSpecByName("BYTES", typeName);
                case DATE:
                    return createSqlDataTypeSpecByName("DATE", typeName);
                case TIME:
                    return createSqlDataTypeSpecByName("TIME", typeName);
                case TIMESTAMP:
                    return createSqlDataTypeSpecByName("TIMESTAMP", typeName);
                default:
                    break;
            }
        }
        return super.getCastSpec(type);
    }

    @Override
    public SqlNode rewriteSingleValueExpr(SqlNode aggCall)
    {
        final SqlNode operand = ((SqlBasicCall) aggCall).operand(0);
        final SqlNode anyValue = SqlStdOperatorTable.ANY_VALUE.createCall(null, SqlParserPos.ZERO, ((SqlBasicCall) aggCall).getOperandList());
        final SqlLiteral nullLiteral = SqlLiteral.createNull(SqlParserPos.ZERO);
        // For BigQuery, generate
        //   CASE COUNT(*)
        //   WHEN 0 THEN NULL
        //   WHEN 1 THEN ANY_VALUE(<result>)
        //   ELSE NULL
        //   END
        final SqlNode caseExpr =
                new SqlCase(SqlParserPos.ZERO,
                        SqlStdOperatorTable.COUNT.createCall(SqlParserPos.ZERO, operand),
                        SqlNodeList.of(
                                SqlLiteral.createExactNumeric("0", SqlParserPos.ZERO),
                                SqlLiteral.createExactNumeric("1", SqlParserPos.ZERO)),
                        SqlNodeList.of(
                                nullLiteral,
                                anyValue),
                        nullLiteral);

        LOG.debug("SINGLE_VALUE rewritten into [{}]", caseExpr);

        return caseExpr;
    }

    @Override
    public StringBuilder quoteIdentifier(
            StringBuilder buf,
            String val)
    {
        // TODO: https://github.com/Canner/canner-metric-layer/issues/55
        if (val.startsWith("$")) {
            return super.quoteIdentifier(buf, "_" + val);
        }
        return super.quoteIdentifier(buf, val);
    }

    private static SqlDataTypeSpec createSqlDataTypeSpecByName(String typeAlias,
            SqlTypeName typeName)
    {
        SqlAlienSystemTypeNameSpec typeNameSpec = new SqlAlienSystemTypeNameSpec(
                typeAlias, typeName, SqlParserPos.ZERO);
        return new SqlDataTypeSpec(typeNameSpec, SqlParserPos.ZERO);
    }

    @Override
    public void unparseCall(final SqlWriter writer, final SqlCall call, final int leftPrec,
            final int rightPrec)
    {
        if (call.getKind() == SqlKind.ITEM) {
            call.operand(0).unparse(writer, leftPrec, rightPrec);
            final SqlWriter.Frame indexFrame = writer.startList("[", "]");
            // BigQuery doesn't support the normal way to access array element like `ARRAY[1,2,3][1]`.
            // It should use `ORDINAL` or `OFFSET` operator to handle index value.
            // Since pg is 1-based array index, that's why we use `ORDINAL` here.
            // https://cloud.google.com/bigquery/docs/reference/standard-sql/arrays#accessing_array_elements
            final SqlWriter.Frame funcFrame = writer.startFunCall("ORDINAL");
            call.operand(1).unparse(writer, leftPrec, rightPrec);
            writer.endFunCall(funcFrame);
            writer.endList(indexFrame);
        }
        else if (call.getOperator().getName().toUpperCase(ENGLISH).equals("DATE_TRUNC")) {
            checkArgument(call.getOperandList().size() == 2, "date_trunc should have only 2 operands");
            List<SqlNode> operandList = call.getOperandList();
            checkArgument(operandList.get(0) instanceof SqlCharStringLiteral, "date_trunc first operand should be a literal");
            String datePart = ((SqlCharStringLiteral) operandList.get(0)).toValue();
            // bigquery DATE_TRUNC(date_expression, date_part) date_part should be an identifier while in pg it's a string literal
            SqlIdentifier datePartIdentifier = new SqlIdentifier(datePart, SqlParserPos.ZERO);
            final SqlWriter.Frame frame = writer.startFunCall("DATE_TRUNC");
            writer.sep(",");
            // bigquery DATE_TRUNC(date_expression, date_part) date_expression should be the first operand while in pg it's the second
            call.operand(1).unparse(writer, leftPrec, rightPrec);
            writer.sep(",");
            datePartIdentifier.unparse(writer, leftPrec, rightPrec);
            writer.endFunCall(frame);
        }
        else if (call.getKind() == SqlKind.WITH_ITEM) {
            final SqlWithItem withItem = (SqlWithItem) call;
            withItem.name.unparse(writer, withItem.getOperator().getLeftPrec(), withItem.getOperator().getRightPrec());
            writer.keyword("AS");
            // calcite SqlWithItem#unparse use 10, if query is union or other that left/right prec > 10,
            // writer won't add parenthesis
            withItem.query.unparse(writer, 100, 100);
        }
        else if (call.getKind() == SqlKind.CAST) {
            checkArgument(call.getOperandList().size() == 2, "CAST should have only 2 operands");
            final SqlWriter.Frame frame = writer.startFunCall(call.getOperator().getName());
            call.operand(0).unparse(writer, 0, 0);
            writer.sep("AS");
            if (call.operand(1) instanceof SqlIntervalQualifier) {
                writer.sep("INTERVAL");
            }
            checkArgument(call.operand(1) instanceof SqlDataTypeSpec, "cast target must be a SqlDataTypeSpec");
            SqlDataTypeSpec sqlDataTypeSpec = call.operand(1);
            writer.sep(toBigQueryTypeLiteral(sqlDataTypeSpec.getTypeNameSpec()));
            writer.endFunCall(frame);
        }
        else {
            super.unparseCall(writer, call, leftPrec, rightPrec);
        }
    }

    private String toBigQueryTypeLiteral(SqlTypeNameSpec sqlTypeNameSpec)
    {
        if (sqlTypeNameSpec instanceof SqlCollectionTypeNameSpec) {
            return toBigQueryCollectionTypeLiteral((SqlCollectionTypeNameSpec) sqlTypeNameSpec);
        }
        if (sqlTypeNameSpec instanceof SqlBasicTypeNameSpec) {
            return toBigQueryBasicTypeLiteral((SqlBasicTypeNameSpec) sqlTypeNameSpec);
        }
        throw new UnsupportedOperationException("Unsupported type: " + sqlTypeNameSpec.getTypeName());
    }

    private String toBigQueryCollectionTypeLiteral(SqlCollectionTypeNameSpec sqlCollectionTypeNameSpec)
    {
        return format("%s<%s>",
                sqlCollectionTypeNameSpec.getTypeName(),
                toBigQueryTypeLiteral(sqlCollectionTypeNameSpec.getElementTypeName()));
    }

    private String toBigQueryBasicTypeLiteral(SqlBasicTypeNameSpec sqlBasicTypeNameSpec)
    {
        SqlTypeName typeName = SqlTypeName.get(sqlBasicTypeNameSpec.getTypeName().getSimple());
        switch (typeName) {
            // BigQuery only supports INT64 for integer types.
            case TINYINT:
            case SMALLINT:
            case INTEGER:
            case BIGINT:
                return "INT64";
            // BigQuery only supports FLOAT64(aka. Double) for floating point types.
            case REAL:
            case FLOAT:
            case DOUBLE:
                return "FLOAT64";
            case DECIMAL:
                if (sqlBasicTypeNameSpec.getPrecision() <= 38) {
                    return "NUMERIC";
                }
                return "BIGNUMERIC";
            case BOOLEAN:
                return "BOOL";
            case CHAR:
            case VARCHAR:
                return "STRING";
            case BINARY:
            case VARBINARY:
                return "BYTES";
            case DATE:
                return "DATE";
            case TIME:
                return "TIME";
            case TIMESTAMP:
                return "TIMESTAMP";
            default:
                throw new UnsupportedOperationException("Unsupported type: " + typeName);
        }
    }
}
