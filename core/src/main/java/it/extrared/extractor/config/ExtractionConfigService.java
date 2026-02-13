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
package it.extrared.extractor.config;

import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.loader.ExtractionConfigRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Service to perform write/read operation over the {@link ExtractionConfiguration} repository. */
@ApplicationScoped
public class ExtractionConfigService {

    @Inject ExtractionConfigRepository repository;

    /**
     * Retrieve the most recent {@link ExtractionConfiguration} persisted.
     *
     * @return the {@link ExtractionConfiguration}.
     */
    public Uni<ExtractionConfiguration> getCurrent() {
        return repository.getCurrentConfiguration();
    }

    /**
     * Adds a new configuration to the repository {@link ExtractionConfiguration}
     *
     * @param configuration the configuration to add.
     * @return a {@link Uni<Void>}
     */
    public Uni<Void> addConfig(ExtractionConfiguration configuration) {
        return repository.addConfiguration(configuration);
    }

    /**
     * Remove the last configuration added to the repository.
     *
     * @return a {@link Uni<Void>}
     */
    public Uni<Void> removeLastConfig() {
        return repository.removeLastConfiguration();
    }
}
