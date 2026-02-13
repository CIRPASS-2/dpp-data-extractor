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
import it.extrared.extractor.ExtractorConfig;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.exceptions.InvalidExtractionConfigException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Chain of configuration loader that executes the loaders by priority order until a configuration
 * is retrieved.
 */
@ApplicationScoped
public class ExtractionConfigLoaderChain {

    private ExtractionConfigLoader head;

    private static final Logger LOG = Logger.getLogger(ExtractionConfigLoaderChain.class);

    @Inject
    public ExtractionConfigLoaderChain(
            Instance<ExtractionConfigLoader> loaders, ExtractorConfig config) {
        if (!loaders.isUnsatisfied()) {
            List<ExtractionConfigLoader> list =
                    loaders.stream()
                            .sorted(Comparator.comparingInt(ExtractionConfigLoader::priority))
                            .toList();
            this.head = buildChain(list);
        } else {
            throw new InvalidExtractionConfigException(
                    "No loader registered to retrieve a configuration");
        }
    }

    public ExtractionConfigLoaderChain() {}

    /**
     * Load the configuration executing loader by loader until one provides it.
     *
     * @return the {@link ExtractionConfiguration} instance.
     */
    public Uni<ExtractionConfiguration> loadConfiguration() {
        Uni<ExtractionConfiguration> uniSchema = head.loadConfig();
        return uniSchema.map(
                s -> {
                    s.validate();
                    return s;
                });
    }

    // builds the chain of configuration loaders.
    private ExtractionConfigLoader buildChain(List<ExtractionConfigLoader> loaders) {
        Iterator<ExtractionConfigLoader> it = loaders.iterator();
        if (!it.hasNext())
            throw new InvalidExtractionConfigException(
                    "No loader registered to retrieve a configuration");
        ExtractionConfigLoader head = it.next();
        ExtractionConfigLoader curr = head;
        while (it.hasNext()) {
            ExtractionConfigLoader next = it.next();
            curr.setNext(next);
            curr = next;
        }
        return head;
    }
}
