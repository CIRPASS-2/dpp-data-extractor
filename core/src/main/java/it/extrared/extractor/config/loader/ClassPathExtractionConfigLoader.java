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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.core.Vertx;
import it.extrared.extractor.ExtractorConfig;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.exceptions.InvalidExtractionConfigException;
import it.extrared.extractor.utils.CommonUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.InputStream;
import java.util.function.Supplier;
import org.jboss.logging.Logger;

/**
 * Implementation of a {@link ExtractionConfigLoader} loading the configuration from the classpath
 * (resources folder).
 */
@Unremovable
@ApplicationScoped
public class ClassPathExtractionConfigLoader extends AbstractExtractionConfigLoader {

    @Inject ExtractorConfig config;
    @Inject ObjectMapper objectMapper;
    @Inject Vertx vertx;

    private static final Logger LOG = Logger.getLogger(ClassPathExtractionConfigLoader.class);

    @Override
    protected Uni<ExtractionConfiguration> loadConfiguration() {
        Supplier<Uni<? extends ExtractionConfiguration>> supplier =
                () -> Uni.createFrom().item(readFromClassPath());
        Uni<ExtractionConfiguration> result = Uni.createFrom().deferred(supplier);
        return result.runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private ExtractionConfiguration readFromClassPath() {
        String resourcePath = "/extraction/%s".formatted(config.config());
        CommonUtils.debug(
                LOG,
                () ->
                        "Trying to load the extraction configuration from class path at %s"
                                .formatted(resourcePath));
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            return objectMapper.readValue(is, ExtractionConfiguration.class);
        } catch (Exception e) {
            throw new InvalidExtractionConfigException(
                    "Error while reading the extraction configuration from class path.");
        }
    }

    @Override
    public Integer priority() {
        return Integer.MAX_VALUE;
    }
}
