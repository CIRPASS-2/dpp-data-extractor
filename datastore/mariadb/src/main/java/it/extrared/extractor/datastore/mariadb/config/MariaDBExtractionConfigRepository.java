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
package it.extrared.extractor.datastore.mariadb.config;

import static it.extrared.extractor.utils.CommonUtils.debug;
import static it.extrared.extractor.utils.SQLClientUtils.getExtractionConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.StringUtils;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.config.loader.ExtractionConfigRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

/** MariaDB implementation for an {@link ExtractionConfigRepository}. */
@ApplicationScoped
public class MariaDBExtractionConfigRepository implements ExtractionConfigRepository {

    @Inject
    @ReactiveDataSource("extraction")
    Pool pool;

    @Inject ObjectMapper objectMapper;

    private static final Logger LOGGER = Logger.getLogger(MariaDBExtractionConfigRepository.class);

    private static final String SELECT_MAX = "SELECT MAX(id) FROM json_configs";

    private static final String SELECT_CURRENT =
                    """
                    SELECT jconfigs.data_config
                    FROM json_configs jconfigs
                    WHERE id = (%s);
    """
                    .formatted(SELECT_MAX);

    private static final String REMOVE_CURRENT =
                    """
                    DELETE
                    FROM json_configs
                    WHERE id = (%s);
    """
                    .formatted(SELECT_MAX);

    private static final String INSERT_SCHEMA =
            """
            INSERT INTO json_configs (data_config,created_at)
            VALUES(?,?);
            """;

    @Override
    public Uni<ExtractionConfiguration> getCurrentConfiguration() {
        debug(LOGGER, () -> "Retrieving current json config");
        Uni<RowSet<byte[]>> result =
                pool.withConnection(
                        c ->
                                c.query(SELECT_CURRENT)
                                        .mapping(
                                                r -> {
                                                    String json = r.getString(0);
                                                    debug(
                                                            LOGGER,
                                                            () ->
                                                                    "Current schema is:\n%s"
                                                                            .formatted(json));
                                                    if (StringUtils.isNotBlank(json))
                                                        return json.getBytes();
                                                    return null;
                                                })
                                        .execute());
        return result.map(
                Unchecked.function(rs -> getExtractionConfig(objectMapper, rs.iterator())));
    }

    @Override
    public Uni<Void> addConfiguration(ExtractionConfiguration schema) {
        try {
            debug(LOGGER, () -> "Persisting a new JSON config..");
            String rawJson = objectMapper.writeValueAsString(schema);
            debug(LOGGER, () -> "JSON config is:\n%s".formatted(rawJson));
            return pool.withTransaction(
                            c ->
                                    c.preparedQuery(INSERT_SCHEMA)
                                            .execute(Tuple.of(rawJson, LocalDateTime.now())))
                    .invoke(rs -> debug(LOGGER, () -> "Insert statement executed"))
                    .replaceWithVoid();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Uni<Void> removeLastConfiguration() {
        debug(LOGGER, () -> "Removing the last JSON config added to the repository...");
        return pool.withTransaction(c -> c.query(REMOVE_CURRENT).execute())
                .invoke(a -> debug(LOGGER, () -> "config deleted"))
                .replaceWithVoid();
    }
}
