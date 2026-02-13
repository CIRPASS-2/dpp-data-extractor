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

import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementation of a {@link ExtractionConfigLoader} loading the configuration from a database
 * using a {@link ExtractionConfigRepository}.
 */
@Unremovable
@ApplicationScoped
public class DBExtractionConfigLoader extends AbstractExtractionConfigLoader {

    @Inject ExtractionConfigRepository dbRepository;

    @Override
    protected Uni<ExtractionConfiguration> loadConfiguration() {
        return dbRepository.getCurrentConfiguration();
    }

    @Override
    public Integer priority() {
        return 99;
    }
}
