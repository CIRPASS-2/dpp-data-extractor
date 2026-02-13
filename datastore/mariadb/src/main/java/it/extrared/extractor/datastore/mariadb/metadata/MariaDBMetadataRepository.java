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
package it.extrared.extractor.datastore.mariadb.metadata;

import static it.extrared.extractor.utils.CommonUtils.debug;
import static it.extrared.extractor.utils.SQLClientUtils.rowToMetadata;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.mutiny.sqlclient.Tuple;
import it.extrared.extractor.ExtractorConfig;
import it.extrared.extractor.registry.DPPMetadataEntry;
import it.extrared.extractor.registry.DPPMetadataRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/** MariaDB implementation of the {@link DPPMetadataRepository} */
@ApplicationScoped
public class MariaDBMetadataRepository implements DPPMetadataRepository {

    @Inject ExtractorConfig config;
    private static final Logger LOG = Logger.getLogger(MariaDBMetadataRepository.class);

    private static final String SELECT_BY_DATETIME_GTEQ =
            """
                            SELECT registry_id,created_at,modified_at,JSON_VALUE(metadata,'$.%s') AS %s, JSON_VALUE(metadata,'$.%s') AS %s
                            FROM dpp_metadata WHERE modified_at>=? AND JSON_VALUE(metadata,'$.%s') = ? ORDER BY modified_at ASC LIMIT 20
            """;

    private static final String SELECT_BY_REGISTRY_IDS =
            """
            SELECT registry_id, JSON_VALUE(metadata,'$.%s') AS %s, JSON_VALUE(metadata,'$.%s') AS %s, created_at, modified_at from  dpp_metadata WHERE registry_id IN(%s) ORDER BY modified_at
            """;

    @Override
    public Uni<List<DPPMetadataEntry>> findByDateGtEq(SqlConnection conn, LocalDateTime dateTime) {
        debug(LOG, () -> "Executing query %s".formatted(SELECT_BY_DATETIME_GTEQ));
        Uni<RowSet<DPPMetadataEntry>> rs =
                conn.preparedQuery(
                                SELECT_BY_DATETIME_GTEQ.formatted(
                                        config.liveURLFieldName(),
                                        config.liveURLFieldName().toLowerCase(),
                                        config.upiFieldName(),
                                        config.upiFieldName().toLowerCase(),
                                        config.granularityFieldName()))
                        .mapping(rowToMetadata(config.liveURLFieldName(), config.upiFieldName()))
                        .execute(Tuple.of(dateTime, config.granularityLevelModel()));
        return rs.map(it -> it.stream().toList());
    }

    @Override
    public Uni<List<DPPMetadataEntry>> findByRegistryIds(
            SqlConnection conn, List<String> registryIds) {
        debug(LOG, () -> "Executing query %s".formatted(SELECT_BY_REGISTRY_IDS));
        String in = registryIds.stream().map(id -> "?").collect(Collectors.joining(","));
        Uni<RowSet<DPPMetadataEntry>> rs =
                conn.preparedQuery(
                                SELECT_BY_REGISTRY_IDS.formatted(
                                        config.liveURLFieldName(),
                                        config.liveURLFieldName().toLowerCase(),
                                        config.upiFieldName(),
                                        config.upiFieldName().toLowerCase(),
                                        in))
                        .mapping(rowToMetadata(config.liveURLFieldName(), config.upiFieldName()))
                        .execute(Tuple.tuple(new ArrayList<>(registryIds)));
        return rs.map(it -> it.stream().toList());
    }
}
