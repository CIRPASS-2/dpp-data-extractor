/*
 * Copyright 2024-2027 CIRPASS-2
 *
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
package it.extrared.extractor.datastore.pgsql.config;

import static it.extrared.extractor.utils.CommonUtils.debug;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.config.loader.ExtractionConfigRepository;
import it.extrared.extractor.utils.JsonUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.Iterator;
import org.jboss.logging.Logger;

/** PgSQL implementation for a {@link ExtractionConfigRepository}. */
@ApplicationScoped
public class PgSQLExtractionConfigRepository implements ExtractionConfigRepository {

    private static final String SELECT_MAX = "SELECT MAX(created_at) FROM json_configs";

    private static final String SELECT_CURRENT =
                    """
                    SELECT jconfigs.data_config
                    FROM json_configs jconfigs
                    WHERE created_at = (%s);
    """
                    .formatted(SELECT_MAX);

    private static final String REMOVE_CURRENT =
                    """
                    DELETE
                    FROM json_configs
                    WHERE created_at = (%s);
    """
                    .formatted(SELECT_MAX);

    private static final String INSERT_SCHEMA =
            """
            INSERT INTO json_configs (data_config,created_at)
            VALUES($1,$2);
            """;

    private static final Logger LOGGER = Logger.getLogger(PgSQLExtractionConfigRepository.class);

    @Inject
    @ReactiveDataSource("extraction")
    Pool pool;

    @Inject ObjectMapper objectMapper;

    @Override
    public Uni<ExtractionConfiguration> getCurrentConfiguration() {
        debug(LOGGER, () -> "Retrieving current JSON config");
        Uni<RowSet<JsonObject>> result =
                pool.withConnection(
                        c -> c.query(SELECT_CURRENT).mapping(r -> r.getJsonObject(0)).execute());
        return result.map(rs -> asConfig(rs.iterator()))
                .invoke(n -> debug(LOGGER, () -> "Retrieve config %s".formatted(n)));
    }

    private ExtractionConfiguration asConfig(Iterator<JsonObject> iterator) {
        if (iterator.hasNext()) {
            JsonObject object = iterator.next();
            if (object != null)
                return objectMapper.convertValue(object.getMap(), ExtractionConfiguration.class);
        }
        return null;
    }

    @Override
    public Uni<Void> addConfiguration(ExtractionConfiguration configuration) {
        debug(LOGGER, () -> "Persisting a new JSON config %s".formatted(configuration));
        return pool.withTransaction(
                        c ->
                                c.preparedQuery(INSERT_SCHEMA)
                                        .execute(
                                                Tuple.of(
                                                        JsonUtils.toVertxJson(configuration),
                                                        LocalDateTime.now())))
                .replaceWithVoid()
                .invoke(v -> debug(LOGGER, () -> "Config was persisted successfully"));
    }

    @Override
    public Uni<Void> removeLastConfiguration() {
        debug(LOGGER, () -> "Removing the last JSON config added to the repository...");
        return pool.withTransaction(c -> c.query(REMOVE_CURRENT).execute()).replaceWithVoid();
    }
}
