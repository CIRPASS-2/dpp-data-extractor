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
import java.util.List;

/** Repository to perform read operation over the registry. */
public interface DPPMetadataRepository {

    /**
     * Retrieves the list of registry entries whose modified date time is equals or is greater than
     * the input date time.
     *
     * @param conn the {@link SqlConnection}
     * @param dateTime the date time to test.
     * @return the list ofm {@link DPPMetadataEntry}
     */
    Uni<List<DPPMetadataEntry>> findByDateGtEq(SqlConnection conn, LocalDateTime dateTime);

    /**
     * Retrieves the list of registry entries whose registry id equals one of the provided.
     *
     * @param conn the {@link SqlConnection}
     * @param registryIds the list of registry id.
     * @return the list of {@link DPPMetadataEntry}
     */
    Uni<List<DPPMetadataEntry>> findByRegistryIds(SqlConnection conn, List<String> registryIds);
}
