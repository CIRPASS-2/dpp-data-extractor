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

/**
 * Base interface for an Extraction configuration loader, able to read an Extraction configuration
 * loader for a given source.
 */
public interface ExtractionConfigLoader {

    /**
     * Load the configuration from the managed source and provides it as an {@link
     * ExtractionConfiguration}.
     *
     * @return the configuration as a {@link Uni<ExtractionConfiguration>}.
     */
    Uni<ExtractionConfiguration> loadConfig();

    /**
     * Return the priority of a configuration loader over the others (the lower the number the
     * greater the priority).
     *
     * @return the priority of a configuration loader as an {@link Integer}.
     */
    Integer priority();

    /**
     * Set the next configuration loader to be executed after this one in case this loader is not
     * able to retrieve the configuration.
     *
     * @param nextLoader the next configuration loader to be executed.
     */
    void setNext(ExtractionConfigLoader nextLoader);
}
