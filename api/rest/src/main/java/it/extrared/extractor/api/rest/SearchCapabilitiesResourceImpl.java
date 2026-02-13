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

import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfigCache;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.config.field.SearchField;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/** Implementation of the SearchCapabilitiesResource endpoint. */
@ApplicationScoped
public class SearchCapabilitiesResourceImpl implements SearchCapabilitiesResource {

    @Inject ExtractionConfigCache configCache;

    @Override
    public Uni<List<SearchField>> getSearchCapabilities() {
        return configCache.get().map(ExtractionConfiguration::getSearchFields);
    }
}
