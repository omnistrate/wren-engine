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

package io.wren.main.connector;

import com.google.inject.Inject;
import io.wren.base.config.ConfigManager;
import io.wren.base.config.WrenConfig;
import io.wren.cache.CacheService;
import io.wren.cache.NOPCacheService;
import io.wren.cache.PathInfo;
import io.wren.main.connector.bigquery.BigQueryCacheService;
import io.wren.main.connector.postgres.PostgresCacheService;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class CacheServiceManager
        implements CacheService
{
    private final ConfigManager configManager;
    private final BigQueryCacheService bigQueryCacheService;
    private final PostgresCacheService postgresCacheService;
    private final CacheService nopCacheService;
    private WrenConfig.DataSourceType dataSourceType;
    private CacheService delegate;

    @Inject
    public CacheServiceManager(
            ConfigManager configManager,
            BigQueryCacheService bigQueryCacheService,
            PostgresCacheService postgresCacheService,
            NOPCacheService nopCacheService)
    {
        this.postgresCacheService = requireNonNull(postgresCacheService, "postgresCacheService is null");
        this.bigQueryCacheService = requireNonNull(bigQueryCacheService, "bigQueryCacheService is null");
        this.nopCacheService = requireNonNull(nopCacheService, "nopCacheService is null");
        this.configManager = requireNonNull(configManager, "configManager is null");
        this.dataSourceType = requireNonNull(configManager.getConfig(WrenConfig.class).getDataSourceType(), "dataSourceType is null");
        changeDelegate(dataSourceType);
    }

    private void changeDelegate(WrenConfig.DataSourceType dataSourceType)
    {
        switch (dataSourceType) {
            case BIGQUERY:
                delegate = bigQueryCacheService;
                break;
            case POSTGRES:
                delegate = postgresCacheService;
                break;
            case DUCKDB, SNOWFLAKE, COUCHBASE:
                delegate = nopCacheService;
                break;

            default:
                throw new UnsupportedOperationException("Unsupported data source type: " + dataSourceType);
        }
    }

    @Override
    public Optional<PathInfo> createCache(String catalog, String schema, String name, String statement)
    {
        return delegate.createCache(catalog, schema, name, statement);
    }

    @Override
    public void deleteTarget(PathInfo pathInfo)
    {
        delegate.deleteTarget(pathInfo);
    }

    public void reload()
    {
        if (dataSourceType != configManager.getConfig(WrenConfig.class).getDataSourceType()) {
            dataSourceType = configManager.getConfig(WrenConfig.class).getDataSourceType();
            changeDelegate(dataSourceType);
        }
    }
}
