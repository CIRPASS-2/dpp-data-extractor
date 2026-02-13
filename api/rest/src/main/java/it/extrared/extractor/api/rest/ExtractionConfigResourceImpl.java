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
package it.extrared.extractor.api.rest;

import static it.extrared.extractor.utils.CommonUtils.debug;

import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfigCache;
import it.extrared.extractor.config.ExtractionConfigService;
import it.extrared.extractor.config.ExtractionConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

/** Implementation of the ExtractionConfigResource endpoints. */
@ApplicationScoped
public class ExtractionConfigResourceImpl implements ExtractionConfigResource {
    @Inject ExtractionConfigService service;
    @Inject ExtractionConfigCache configCache;

    private static final Logger LOGGER = Logger.getLogger(ExtractionConfigResourceImpl.class);

    @Override
    public Uni<RestResponse<Void>> addConfig(ExtractionConfiguration config) {
        debug(
                LOGGER,
                () ->
                        "Controller method to add a new Extraction config invoked with body \n%s"
                                .formatted(config));
        return service.addConfig(config)
                .invoke(r -> configCache.invalidate())
                .map(n -> RestResponse.status(201));
    }

    @Override
    public Uni<RestResponse<ExtractionConfiguration>> getCurrent() {
        debug(LOGGER, () -> "Controller method to get current Extraction config invoked");
        return service.getCurrent()
                .map(RestResponse::ok)
                .invoke(r -> r.getHeaders().add("Content-Type", "application/json"));
    }

    @Override
    public Uni<RestResponse<Void>> removeCurrent() {
        debug(LOGGER, () -> "Controller method to remove current Extraction config invoked");
        Uni<RestResponse<Void>> res = service.removeLastConfig().map(v -> RestResponse.noContent());
        return res.invoke(r -> configCache.invalidate());
    }
}
