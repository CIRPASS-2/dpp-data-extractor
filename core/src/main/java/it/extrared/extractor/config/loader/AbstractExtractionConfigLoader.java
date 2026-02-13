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

import static it.extrared.extractor.utils.CommonUtils.debug;

import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfiguration;
import java.util.function.Function;
import org.jboss.logging.Logger;

/**
 * Abstract implementation of a {@link it.extrared.extractor.config.ExtractionConfiguration}
 * providing reusable logic between different configuration loaders.
 */
public abstract class AbstractExtractionConfigLoader implements ExtractionConfigLoader {

    protected ExtractionConfigLoader next;

    private static final Logger LOG = Logger.getLogger(AbstractExtractionConfigLoader.class);

    @Override
    public void setNext(ExtractionConfigLoader nextLoader) {
        this.next = nextLoader;
    }

    @Override
    public Uni<ExtractionConfiguration> loadConfig() {
        Uni<ExtractionConfiguration> schema = loadConfiguration();
        Function<ExtractionConfiguration, Uni<? extends ExtractionConfiguration>> doNextWhenNull =
                jn -> {
                    if (jn == null) {
                        debug(
                                LOG,
                                () ->
                                        "No configuration found with loader %s, trying next loader..."
                                                .formatted(getClass().getName()));
                        return next.loadConfig();
                    } else {
                        debug(
                                LOG,
                                () ->
                                        "Configuration found with loader %s"
                                                .formatted(getClass().getName()));
                        return Uni.createFrom().item(() -> jn);
                    }
                };
        return schema.flatMap(doNextWhenNull);
    }

    /**
     * Subclasses must implement this method to perform their custom logic that actually loads the
     * configuration.
     *
     * @return a configuration as a {@link Uni<ExtractionConfiguration>}
     */
    protected abstract Uni<ExtractionConfiguration> loadConfiguration();
}
