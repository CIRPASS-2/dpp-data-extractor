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
package it.extrared.extractor.datastore.mariadb.extraction;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.mutiny.sqlclient.Tuple;
import it.extrared.extractor.failures.ExtractionFailure;
import it.extrared.extractor.failures.ExtractionFailureRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.function.Function;

/** MariaDB implementation for an {@link ExtractionFailureRepository}. */
@ApplicationScoped
public class MariaDBExtractionFailureRepository implements ExtractionFailureRepository {

    private static final String UPSERT =
            """
            INSERT INTO extraction_failures (registry_id, retrials)
            VALUES (?, 0)
            ON DUPLICATE KEY UPDATE
                retrials = retrials + 1;
            """;

    private static final String SELECT_BY_REG_ID =
            """
            SELECT id,registry_id,retrials FROM extraction_failures WHERE registry_id = ?
            """;

    private static final String ALL_EXTRACTIONS_QUERIES =
            """
            SELECT id, registry_id, retrials FROM extraction_failures
            """;

    private static final String DELETE =
            """
            DELETE FROM extraction_failures WHERE id=?
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
        res =
                res.flatMap(
                        rs ->
                                conn.preparedQuery(SELECT_BY_REG_ID)
                                        .execute(Tuple.of(extractionFailure.getRegistryId())));
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
