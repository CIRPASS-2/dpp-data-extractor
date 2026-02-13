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

import static it.extrared.extractor.utils.CommonUtils.debug;

import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.loader.ExtractionConfigLoaderChain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicReference;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ExtractionConfigCache {

    private final AtomicReference<Uni<ExtractionConfiguration>> cached = new AtomicReference<>();

    private static final Logger LOG = Logger.getLogger(ExtractionConfigCache.class);

    @Inject ExtractionConfigLoaderChain loaderChain;

    /**
     * @return the currently cached configuration. If cache is empty the configuration is first
     *     loaded and then cached.
     */
    public Uni<ExtractionConfiguration> get() {
        Uni<ExtractionConfiguration> config = cached.get();
        if (config == null) {
            debug(LOG, () -> "Caching extraction configuration...");
            config = loaderChain.loadConfiguration().memoize().indefinitely();
            cached.compareAndSet(null, config);
            debug(LOG, () -> "Extraction configuration cached...");
        }
        return config;
    }

    /** Invalidates the cache. */
    public void invalidate() {
        debug(LOG, () -> "Invalidating Extraction config cache...");
        cached.set(null);
    }
}
