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

package io.wren.base.config;

import io.airlift.configuration.Config;

import java.util.Optional;

public class CouchbaseConfig
{
    public static final String COUCHBASE_JDBC_URL = "couchbase.jdbc.url";
    public static final String COUCHBASE_SERVER = "couchbase.server";
    public static final String COUCHBASE_USER = "couchbase.user";
    public static final String COUCHBASE_PASSWORD = "couchbase.password";
    public static final String COUCHBASE_N1QL_PORT = "couchbase.N1QLPort";
    public static final String COUCHBASE_USE_SSL = "couchbase.useSSL";

    private String jdbcUrl = " jdbc:couchbase:";
    private String user;
    private String password;
    private String server;
    private String n1QLPort = "";
    private Boolean useSSL = Boolean.TRUE;

    public String getJdbcUrl()
    {
        return jdbcUrl;
    }

    @Config(COUCHBASE_JDBC_URL)
    public CouchbaseConfig setJdbcUrl(String jdbcUrl)
    {
        this.jdbcUrl = jdbcUrl;
        return this;
    }

    public String getServer()
    {
        return server;
    }

    @Config(COUCHBASE_SERVER)
    public CouchbaseConfig setServer(String jdbcUrl)
    {
        this.server = jdbcUrl;
        return this;
    }

    public String getUser()
    {
        return user;
    }

    @Config(COUCHBASE_USER)
    public CouchbaseConfig setUser(String user)
    {
        this.user = user;
        return this;
    }

    public String getPassword()
    {
        return password;
    }

    @Config(COUCHBASE_PASSWORD)
    public CouchbaseConfig setPassword(String password)
    {
        this.password = password;
        return this;
    }

    public Optional<String> getN1QLPort()
    {
        return Optional.ofNullable(n1QLPort);
    }

    @Config(COUCHBASE_N1QL_PORT)
    public CouchbaseConfig setN1QLPort(String n1QLPort)
    {
        this.n1QLPort = n1QLPort;
        return this;
    }

    public Optional<Boolean> getUseSSL()
    {
        return Optional.ofNullable(useSSL);
    }

    @Config(COUCHBASE_USE_SSL)
    public CouchbaseConfig setUseSSL(Boolean useSSL)
    {
        this.useSSL = useSSL;
        return this;
    }
}
