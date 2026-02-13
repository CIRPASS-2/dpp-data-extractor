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
package it.extrared.extractor.datastore.pgsql.extraction;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.mutiny.sqlclient.Tuple;
import it.extrared.extractor.registry.ExtractionRegistryEntry;
import it.extrared.extractor.registry.ExtractionRegistryRepository;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.function.Function;

@Priority(1)
@ApplicationScoped
public class PgSQLExtractionRegistryRepository implements ExtractionRegistryRepository {

    private static final String GET =
            """
            SELECT id, processed_until FROM extraction_registry LIMIT 1
            """;

    private static final String UPDATE_DATE =
            """
            UPDATE extraction_registry SET processed_until=$1
            """;

    private static final String INSERT =
            """
            INSERT INTO extraction_registry (processed_until) VALUES($1)
            """;

    private static final Function<Row, ExtractionRegistryEntry> MAPPER =
            r -> {
                ExtractionRegistryEntry entry = new ExtractionRegistryEntry();
                entry.setId(r.getLong("id"));
                entry.setProcessedUntil(r.getLocalDateTime("processed_until"));
                return entry;
            };

    @Override
    public Uni<ExtractionRegistryEntry> get(SqlConnection conn) {
        Uni<Optional<ExtractionRegistryEntry>> entry =
                conn.preparedQuery(GET).execute().map(r -> r.stream().map(MAPPER).findFirst());
        return entry.flatMap(
                op -> {
                    if (op.isPresent()) return Uni.createFrom().item(op.get());
                    return insert(conn);
                });
    }

    private Uni<ExtractionRegistryEntry> insert(SqlConnection conn) {
        LocalDateTime dateTime = LocalDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIN);
        return conn.preparedQuery(INSERT)
                .execute(Tuple.of(dateTime))
                .map(
                        rs -> {
                            ExtractionRegistryEntry res = new ExtractionRegistryEntry();
                            res.setProcessedUntil(dateTime);
                            return res;
                        });
    }

    @Override
    public Uni<Void> updateDateTime(SqlConnection conn, LocalDateTime dateTime) {
        return conn.preparedQuery(UPDATE_DATE).execute(Tuple.of(dateTime)).replaceWithVoid();
    }
}
