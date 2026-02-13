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

import static it.extrared.extractor.utils.JsonUtils.extractNamespace;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import it.extrared.extractor.ExtractorConfig;
import it.extrared.extractor.config.ExtractionConfigCache;
import it.extrared.extractor.exceptions.InvalidDPPException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.json.*;
import java.io.InputStream;
import java.util.*;
import org.jboss.logging.Logger;

/**
 * Performs the extraction of values to be persisted in the {@link
 * it.extrared.extractor.data.SearchDataRepository} from a DPP.
 */
@ApplicationScoped
public class SearchKeyExtractor {

    private static final String CONTEXT = "@context";
    private static final String VOCAB = "@vocab";
    private static final Logger LOGGER = Logger.getLogger(SearchKeyExtractor.class);

    @Inject ExtractionConfigCache configCache;

    @Inject ExtractorConfig config;

    @Inject Instance<SearchKeyExtractorStrategy> searchKeyExtractors;

    /**
     * Extract the data from the DPP. It analyze the DPP to determine the proper {@link
     * SearchKeyExtractorStrategy} and then execute it.
     *
     * @param inputStream the {@link InputStream} of the DPP from which extract data.
     * @return a Uni with a Map containing key value pairs with extracted data where the key is
     *     equal to the corresponding field listed in the configuration as a {@link
     *     it.extrared.extractor.config.field.SearchField} and the value is the extracted value.
     */
    public Uni<Map<String, Object>> extractSearchKeys(InputStream inputStream) {
        try {
            JsonDocument document = JsonDocument.of(inputStream);
            Uni<ExtractionStrategyType> strategyType =
                    Uni.createFrom()
                            .deferred(
                                    Unchecked.supplier(
                                            () ->
                                                    Uni.createFrom()
                                                            .item(detectFromDocument(document))));
            strategyType = strategyType.runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
            Uni<SearchKeyExtractorStrategy> extractor = strategyType.map(this::selectExtractor);
            return configCache
                    .get()
                    .flatMap(c -> extractor.flatMap(e -> e.extractSearchKeys(c, document)));
        } catch (JsonLdError e) {
            throw new InvalidDPPException("Invalid JSON", e);
        }
    }

    private SearchKeyExtractorStrategy selectExtractor(ExtractionStrategyType type) {
        for (SearchKeyExtractorStrategy extractor : searchKeyExtractors) {
            if (extractor.canHandle(type)) return extractor;
        }
        throw new UnsupportedOperationException(
                "Unable to find a selection strategy from the given payload");
    }

    private ExtractionStrategyType detectFromDocument(JsonDocument document) throws JsonLdError {

        JsonStructure structure =
                document.getJsonContent()
                        .orElseThrow(() -> new InvalidDPPException("Empty document"));

        if (!(structure instanceof JsonObject jsonLd)) {
            return ExtractionStrategyType.PLAIN_JSON;
        }

        if (!jsonLd.containsKey(CONTEXT)) {
            return ExtractionStrategyType.PLAIN_JSON;
        }

        JsonValue contextValue = jsonLd.get(CONTEXT);
        Optional<List<String>> refContexts = config.dpp().referenceOntology().contexts();
        // try determining if the ontology is the known one based on the context uri
        if (refContexts.isPresent() && matchesContextContent(contextValue, refContexts.get())) {
            return ExtractionStrategyType.KNOW_ONTOLOGY;
        }

        // fallback on extracting the most common namespace in the document and uses that one as the
        // ref vocabulary to be checked.
        if (checkVocabularyOnNamespaces(JsonLd.expand(document).get())) {
            return ExtractionStrategyType.KNOW_ONTOLOGY;
        }

        return ExtractionStrategyType.UNKNOWN_ONTOLOGY;
    }

    private boolean matchesContextContent(JsonValue contextValue, List<String> contexts) {

        // context uri match
        if (contextValue.getValueType() == JsonValue.ValueType.STRING) {
            String contextUri = ((JsonString) contextValue).getString();
            return contexts.stream().anyMatch(c -> Objects.equals(c, contextUri));
        }

        // context might be array of uri lets check each.
        if (contextValue.getValueType() == JsonValue.ValueType.ARRAY) {
            JsonArray contextArray = (JsonArray) contextValue;
            for (JsonValue item : contextArray) {
                if (item.getValueType() == JsonValue.ValueType.STRING) {
                    String contextUri = ((JsonString) item).getString();
                    if (contexts.stream().anyMatch(c -> Objects.equals(c, contextUri))) {
                        return true;
                    }
                }
            }
        }

        // try with the @vocab if present inside the context
        if (contextValue.getValueType() == JsonValue.ValueType.OBJECT) {
            JsonObject contextObj = (JsonObject) contextValue;

            if (contextObj.containsKey(VOCAB)) {
                String vocab = contextObj.getString(VOCAB);
                return isReferenceVocab(vocab);
            }
        }

        return false;
    }

    private boolean checkVocabularyOnNamespaces(JsonArray expanded) {
        Map<String, Integer> counts = new HashMap<>();
        countNamespacesInExpanded(expanded, counts);
        Optional<String> mostUsed =
                counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey);
        return mostUsed.flatMap(
                        s ->
                                config.dpp()
                                        .referenceOntology()
                                        .vocabularies()
                                        .map(l -> l.stream().anyMatch(v -> Objects.equals(v, s))))
                .orElse(false);
    }

    private boolean isReferenceVocab(String uri) {

        Optional<List<String>> refNs = config.dpp().referenceOntology().vocabularies();
        if (refNs.isPresent()) {
            for (String namespace : refNs.get()) if (uri.startsWith(namespace)) return true;
            return false;
        }
        return false;
    }

    private void countNamespacesInExpanded(
            JsonArray expanded, Map<String, Integer> namespaceCounts) {

        for (JsonValue item : expanded) {
            if (item.getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObject obj = item.asJsonObject();
                countNamespacesInObject(obj, namespaceCounts);
            }
        }
    }

    private void countNamespacesInObject(JsonObject obj, Map<String, Integer> namespaceCounts) {

        for (String key : obj.keySet()) {
            if (key.startsWith("@")) {
                continue;
            }
            String namespace = extractNamespace(key);
            if (namespace != null) {
                namespaceCounts.merge(namespace, 1, Integer::sum);
            }

            JsonValue value = obj.get(key);

            if (value.getValueType() == JsonValue.ValueType.ARRAY) {
                JsonArray array = value.asJsonArray();
                for (JsonValue arrayItem : array) {
                    if (arrayItem.getValueType() == JsonValue.ValueType.OBJECT) {
                        countNamespacesInObject(arrayItem.asJsonObject(), namespaceCounts);
                    }
                }
            } else if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                countNamespacesInObject(value.asJsonObject(), namespaceCounts);
            }
        }
    }
}
