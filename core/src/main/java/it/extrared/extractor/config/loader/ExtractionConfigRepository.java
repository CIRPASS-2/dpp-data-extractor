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

import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfiguration;

/** Base interface for extraction configuration repository. */
public interface ExtractionConfigRepository {

    /**
     * Get the currently active configuration, i.e. the last one added.
     *
     * @return the configuration as a {@link Uni<ExtractionConfiguration>}
     */
    Uni<ExtractionConfiguration> getCurrentConfiguration();

    /**
     * Add a new extraction configuration to the repository.
     *
     * @param configuration the configuration as a {@link Uni<ExtractionConfiguration>}.
     * @return empty as a {@link Uni<Void>}
     */
    Uni<Void> addConfiguration(ExtractionConfiguration configuration);

    /**
     * Remove the last added extraction configuration the currently active one.
     *
     * @return empty result as a {@link Uni<Void>}.
     */
    Uni<Void> removeLastConfiguration();
}
