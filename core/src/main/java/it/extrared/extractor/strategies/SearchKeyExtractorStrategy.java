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
package it.extrared.extractor.strategies;

import com.apicatalog.jsonld.document.JsonDocument;
import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfiguration;
import java.util.Map;

/** Base interface for an extraction strategy. */
public interface SearchKeyExtractorStrategy {

    /**
     * @param type the type of extraction.
     * @return true if this strategy supports the type false otherwise.
     */
    boolean canHandle(ExtractionStrategyType type);

    /**
     * Performs the search keys extraction.
     *
     * @param configuration the extraction configuration.
     * @param jsonDocument the json document representing the DPP.
     * @return a Uni with a Map containing key value pairs with extracted data where the key is
     *     equal to the corresponding field listed in the configuration as a {@link
     *     it.extrared.extractor.config.field.SearchField} and the value is the extracted value.
     */
    Uni<Map<String, Object>> extractSearchKeys(
            ExtractionConfiguration configuration, JsonDocument jsonDocument);
}
