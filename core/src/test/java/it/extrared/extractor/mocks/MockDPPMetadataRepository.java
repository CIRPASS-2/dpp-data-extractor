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
package it.extrared.extractor.mocks;

import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import it.extrared.extractor.registry.DPPMetadataEntry;
import it.extrared.extractor.registry.DPPMetadataRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@DefaultBean
@ApplicationScoped
public class MockDPPMetadataRepository implements DPPMetadataRepository {

    @Override
    public Uni<List<DPPMetadataEntry>> findByDateGtEq(SqlConnection conn, LocalDateTime dateTime) {
        DPPMetadataEntry entry1 = new DPPMetadataEntry();
        entry1.setLiveURL("http://localhost:8080/dpp1");
        entry1.setUpi("12345678");
        entry1.setModifiedAt(LocalDateTime.now());
        entry1.setRegistryId(UUID.randomUUID().toString());
        DPPMetadataEntry entry2 = new DPPMetadataEntry();
        entry2.setLiveURL("http://localhost:8080/dpp2");
        entry2.setUpi("87654321");
        entry2.setModifiedAt(LocalDateTime.now());
        entry2.setRegistryId(UUID.randomUUID().toString());
        return Uni.createFrom().item(List.of(entry1, entry2));
    }

    @Override
    public Uni<List<DPPMetadataEntry>> findByRegistryIds(
            SqlConnection conn, List<String> registryIds) {
        DPPMetadataEntry entry1 = new DPPMetadataEntry();
        entry1.setLiveURL("http://localhost:8080/dpp3");
        entry1.setUpi("76544567");
        entry1.setModifiedAt(LocalDateTime.now());
        entry1.setRegistryId(registryIds.getFirst());
        return Uni.createFrom().item(List.of(entry1));
    }
}
