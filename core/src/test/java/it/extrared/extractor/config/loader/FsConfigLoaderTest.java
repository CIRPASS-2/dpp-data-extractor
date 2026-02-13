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
package it.extrared.extractor.config.loader;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfigCache;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.utils.JsonUtils;
import jakarta.inject.Inject;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
@TestProfile(JsonSchemaSystemPropertyProfile.class)
public class FsConfigLoaderTest {

    @Inject ExtractionConfigCache configCache;

    @InjectMock ExtractionConfigRepository repository;

    @BeforeEach
    public void before() {
        configCache.invalidate();
    }

    @Test
    @RunOnVertxContext
    public void testEnvLoading(UniAsserter asserter) throws IOException {
        Mockito.when(repository.getCurrentConfiguration()).thenReturn(Uni.createFrom().nullItem());
        ExtractionConfiguration test =
                JsonUtils.loadClasspathJsonAs(
                        "fs-test-core-extraction.json", ExtractionConfiguration.class);
        asserter.assertEquals(() -> configCache.get(), test);
    }
}
