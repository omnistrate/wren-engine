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

package io.wren.main.web;

import com.google.common.collect.ImmutableList;
import io.wren.base.ConnectorRecordIterator;
import io.wren.base.WrenException;
import io.wren.main.connector.couchbase.CouchbaseMetadata;
import io.wren.main.web.dto.QueryResultDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;

import static io.wren.base.metadata.StandardErrorCode.GENERIC_INTERNAL_ERROR;
import static io.wren.main.web.WrenExceptionMapper.bindAsyncResponse;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Path("/v1/data-source/couchbase")
public class CouchbaseResource
{
    private final CouchbaseMetadata metadata;

    @Inject
    public CouchbaseResource(CouchbaseMetadata metadata)
    {
        this.metadata = requireNonNull(metadata, "metadata is null");
    }

    @POST
    @Path("/query")
    @Produces(APPLICATION_JSON)
    public void query(
            String statement,
            @Suspended AsyncResponse asyncResponse)
            throws Exception
    {
        supplyAsync(() ->
        {
            try (ConnectorRecordIterator iterator = metadata.directQuery(statement, ImmutableList.of())) {
                ImmutableList.Builder<Object[]> data = ImmutableList.builder();
                while (iterator.hasNext()) {
                    data.add(iterator.next());
                }
                return new QueryResultDto(iterator.getColumns(), data.build());
            }
            catch (Exception e)
            {
                throw new WrenException(GENERIC_INTERNAL_ERROR, e.getMessage());
            }
        }).whenComplete(bindAsyncResponse(asyncResponse));
    }

    @GET
    @Path("/schema")
    @Produces(APPLICATION_JSON)
    public void getSchema(@Suspended AsyncResponse asyncResponse)
    {
        supplyAsync(metadata::getSchema).whenComplete(bindAsyncResponse(asyncResponse));
    }

    @GET
    @Path("/settings/init-sql")
    public void getInitSQL(@Suspended AsyncResponse asyncResponse)
    {
        supplyAsync(() -> "SELECT 1;").whenComplete(bindAsyncResponse(asyncResponse));
    }

    @PUT
    @Path("/settings/init-sql")
    public void setInitSQL(String sql, @Suspended AsyncResponse asyncResponse)
    {
        supplyAsync(() -> null).whenComplete(bindAsyncResponse(asyncResponse));
    }

    @PATCH
    @Path("/settings/init-sql")
    public void updateInitSQL(String sql, @Suspended AsyncResponse asyncResponse)
    {
        supplyAsync(() -> null).whenComplete(bindAsyncResponse(asyncResponse));
    }

    @GET
    @Path("/settings/session-sql")
    public void appendToInitSQL(@Suspended AsyncResponse asyncResponse)
    {
        supplyAsync(() -> "SELECT 1;").whenComplete(bindAsyncResponse(asyncResponse));
    }

    @PUT
    @Path("/settings/session-sql")
    public void setSessionSQL(String sql, @Suspended AsyncResponse asyncResponse)
    {
        supplyAsync(() -> null).whenComplete(bindAsyncResponse(asyncResponse));
    }

    @PATCH
    @Path("/settings/session-sql")
    public void appendToSessionSQL(String sql, @Suspended AsyncResponse asyncResponse)
    {
        supplyAsync(() -> null).whenComplete(bindAsyncResponse(asyncResponse));
    }
}
