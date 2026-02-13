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
package it.extrared.extractor.registry;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import java.time.LocalDateTime;

/** Repository for the {@link ExtractionRegistryEntry}. */
public interface ExtractionRegistryRepository {

    /**
     * Retrieve the entry. Creates it if not exists.
     *
     * @param conn the {@link SqlConnection}
     * @return the current entry.
     */
    Uni<ExtractionRegistryEntry> get(SqlConnection conn);

    /**
     * Update the dateTime of the last processed value.
     *
     * @param conn the {@link SqlConnection}
     * @param dateTime the dateTime to set.
     * @return
     */
    Uni<Void> updateDateTime(SqlConnection conn, LocalDateTime dateTime);
}
