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
package it.extrared.extractor.datastore.pgsql.metadata;

import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import it.extrared.extractor.registry.DPPMetadataEntry;
import it.extrared.extractor.registry.DPPMetadataRepository;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PgSQLDPPMetadataRepositoryTest {

    @Inject Pool pool;

    @Inject DPPMetadataRepository repository;

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testSelectByGtEqDateTime(UniAsserter uniAsserter) {
        LocalDateTime testDateTime = LocalDateTime.now().minusDays(1L);
        Uni<List<DPPMetadataEntry>> entries =
                pool.withConnection(c -> repository.findByDateGtEq(c, testDateTime));
        uniAsserter.assertThat(
                () -> entries,
                l -> {
                    assertEquals(3, l.size());
                    l.forEach(
                            e -> {
                                assertNotNull(e.getLiveURL());
                                assertNotNull(e.getRegistryId());
                                assertNotNull(e.getUpi());
                                assertTrue(testDateTime.isBefore(e.getModifiedAt()));
                            });
                });
    }

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testSelectByRegistryIdIn(UniAsserter uniAsserter) {
        List<String> ids = List.of("550e8400-e29b-41d4-a716-446655440000");
        Uni<List<DPPMetadataEntry>> entries =
                pool.withConnection(c -> repository.findByRegistryIds(c, ids));
        uniAsserter.assertThat(
                () -> entries,
                l -> {
                    assertEquals(1, l.size());
                    l.forEach(
                            e -> {
                                assertNotNull(e.getLiveURL());
                                assertNotNull(e.getRegistryId());
                                assertNotNull(e.getUpi());
                                assertTrue(ids.contains(e.getRegistryId()));
                            });
                });
    }
}
