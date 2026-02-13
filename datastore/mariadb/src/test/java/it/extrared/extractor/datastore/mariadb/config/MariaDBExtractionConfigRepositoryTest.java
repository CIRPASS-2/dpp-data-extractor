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

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.config.loader.ExtractionConfigRepository;
import it.extrared.extractor.utils.JsonUtils;
import jakarta.inject.Inject;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MariaDBExtractionConfigRepositoryTest {

    @Inject ExtractionConfigRepository configRepository;

    @Test
    @RunOnVertxContext
    public void testAddNewGetRemoveGet(UniAsserter asserter) throws IOException {
        ExtractionConfiguration config =
                JsonUtils.loadClasspathJsonAs(
                        "db-test-core-extraction.json", ExtractionConfiguration.class);
        ExtractionConfiguration config2 =
                JsonUtils.loadClasspathJsonAs(
                        "db-2-test-core-extraction.json", ExtractionConfiguration.class);
        asserter.assertNull(() -> configRepository.addConfiguration(config));
        asserter.assertNull(() -> configRepository.addConfiguration(config2));
        asserter.assertEquals(() -> configRepository.getCurrentConfiguration(), config2);
        asserter.assertEquals(
                () ->
                        configRepository
                                .removeLastConfiguration()
                                .flatMap(v -> configRepository.getCurrentConfiguration()),
                config);
    }
}
