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
package it.extrared.extractor.failures;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import java.util.List;

/** Repository to persist information about an extraction failure and enable retrials. */
public interface ExtractionFailureRepository {

    /**
     * Retrieve the list of extraction failures.
     *
     * @param conn the {@link SqlConnection}.
     * @return the list of extraction failures.
     */
    Uni<List<ExtractionFailure>> getExtractionFailures(SqlConnection conn);

    /**
     * Delete an extration failure.
     *
     * @param conn the {@link SqlConnection}
     * @param id the id of the failure to delete.
     * @return a {@link Uni<Void>}
     */
    Uni<Void> deleteExtractionFailure(SqlConnection conn, Long id);

    /**
     * Creates a new extraction failure or if it exists increase the number of failure.
     *
     * @param conn the {@link SqlConnection}
     * @param extractionFailure the {@link ExtractionFailure} to be persisted or updated.
     * @return a {@link Uni<ExtractionFailure>}
     */
    Uni<ExtractionFailure> createOrIncrease(
            SqlConnection conn, ExtractionFailure extractionFailure);
}
