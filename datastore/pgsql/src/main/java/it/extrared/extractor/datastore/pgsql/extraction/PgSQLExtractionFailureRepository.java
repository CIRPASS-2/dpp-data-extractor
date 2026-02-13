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
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.sqlclient.*;
import it.extrared.extractor.failures.ExtractionFailure;
import it.extrared.extractor.failures.ExtractionFailureRepository;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import java.util.List;
import java.util.function.Function;

/** PgSQL implementation for a {@link ExtractionFailureRepository}. */
@Priority(1)
@Default
@ApplicationScoped
public class PgSQLExtractionFailureRepository implements ExtractionFailureRepository {

    private static final String ALL_EXTRACTIONS_QUERIES =
            """
            SELECT id, registry_id, retrials FROM extraction_failures
            """;

    private static final String DELETE =
            """
            DELETE FROM extraction_failures WHERE id=$1
            """;

    private static final String UPSERT =
            """
            INSERT INTO extraction_failures (registry_id, retrials)
            VALUES ($1, 0)
            ON CONFLICT (registry_id)
            DO UPDATE SET
            retrials = extraction_failures.retrials + 1
            RETURNING extraction_failures.id, extraction_failures.registry_id, extraction_failures.retrials
            """;
    private static final Function<Row, ExtractionFailure> AS_EXTRACTION_FAILURE =
            Unchecked.function(
                    r -> {
                        ExtractionFailure extractionFailure = new ExtractionFailure();
                        extractionFailure.setId(r.getLong("id"));
                        extractionFailure.setRetrials(r.getInteger("retrials"));
                        extractionFailure.setRegistryId(r.getString("registry_id"));
                        return extractionFailure;
                    });

    @Override
    public Uni<List<ExtractionFailure>> getExtractionFailures(SqlConnection conn) {
        return conn.preparedQuery(ALL_EXTRACTIONS_QUERIES)
                .execute()
                .map(rs -> rs.stream().map(AS_EXTRACTION_FAILURE).toList());
    }

    @Override
    public Uni<Void> deleteExtractionFailure(SqlConnection conn, Long id) {
        return conn.preparedQuery(DELETE).execute(Tuple.of(id)).replaceWithVoid();
    }

    @Override
    public Uni<ExtractionFailure> createOrIncrease(
            SqlConnection conn, ExtractionFailure extractionFailure) {
        Uni<RowSet<Row>> res =
                conn.preparedQuery(UPSERT).execute(Tuple.of(extractionFailure.getRegistryId()));
        return res.map(
                rs ->
                        rs.stream()
                                .map(AS_EXTRACTION_FAILURE)
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                new RuntimeException(
                                                        "Expected to return a record, got none")));
    }
}
