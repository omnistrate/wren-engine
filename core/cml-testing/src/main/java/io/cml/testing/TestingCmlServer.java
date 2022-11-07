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

package io.cml.testing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import com.google.common.net.HostAndPort;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.bootstrap.LifeCycleManager;
import io.airlift.event.client.EventModule;
import io.airlift.http.server.testing.TestingHttpServer;
import io.airlift.http.server.testing.TestingHttpServerModule;
import io.airlift.jaxrs.JaxrsModule;
import io.airlift.json.JsonModule;
import io.airlift.node.NodeModule;
import io.cml.metrics.MetricResourceModule;
import io.cml.server.module.BigQueryConnectorModule;
import io.cml.server.module.PostgresWireProtocolModule;
import io.cml.wireprotocol.PostgresNetty;
import io.cml.wireprotocol.ssl.EmptyTlsDataProvider;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static io.cml.PostgresWireProtocolConfig.PG_WIRE_PROTOCOL_PORT;

public class TestingCmlServer
        implements Closeable
{
    private static final String HTTP_SERVER_PORT = "http-server.http.port";
    private static final String NODE_ENVIRONMENT = "node.environment";
    private final Injector injector;
    private final Closer closer = Closer.create();

    public static Builder builder()
    {
        return new Builder();
    }

    private TestingCmlServer(Map<String, String> requiredConfigs)
            throws IOException
    {
        Map<String, String> requiredConfigProps = new HashMap<>();
        requiredConfigProps.put(PG_WIRE_PROTOCOL_PORT, String.valueOf(randomPort()));
        requiredConfigProps.put(HTTP_SERVER_PORT, String.valueOf(randomPort()));
        requiredConfigProps.put(NODE_ENVIRONMENT, "test");

        requiredConfigProps.putAll(requiredConfigs);

        Bootstrap app = new Bootstrap(ImmutableList.<Module>of(
                new TestingHttpServerModule(),
                new NodeModule(),
                new JsonModule(),
                new JaxrsModule(),
                new EventModule(),
                new PostgresWireProtocolModule(new EmptyTlsDataProvider()),
                new BigQueryConnectorModule(),
                new MetricResourceModule()));

        injector = app
                .doNotInitializeLogging()
                .setRequiredConfigurationProperties(requiredConfigProps)
                .quiet()
                .initialize();

        closer.register(() -> injector.getInstance(LifeCycleManager.class).stop());
    }

    @VisibleForTesting
    public HostAndPort getPgHostAndPort()
    {
        return injector.getInstance(PostgresNetty.class).getHostAndPort();
    }

    public URI getHttpServerBasedUrl()
    {
        return injector.getInstance(TestingHttpServer.class).getBaseUrl();
    }

    @Override
    public void close()
            throws IOException
    {
        closer.close();
    }

    private static int randomPort()
            throws IOException
    {
        // ServerSocket(0) results in availability of a free random port
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    public static class Builder
    {
        private Map<String, String> configs = new HashMap<>();

        public Builder setRequireConfig(String key, String value)
        {
            this.configs.put(key, value);
            return this;
        }

        public Builder setRequiredConfigs(Map<String, String> configs)
        {
            this.configs = configs;
            return this;
        }

        public TestingCmlServer build()
        {
            try {
                return new TestingCmlServer(configs);
            }
            catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}