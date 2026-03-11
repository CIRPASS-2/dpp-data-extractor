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

import static it.extrared.extractor.utils.CommonUtils.warn;

import com.apicatalog.jsonld.document.JsonDocument;
import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.config.NoOntology;
import it.extrared.extractor.config.field.FieldType;
import it.extrared.extractor.config.field.NoOntologyFieldSpec;
import it.extrared.extractor.config.field.VariantsWithContext;
import it.extrared.extractor.utils.JsonUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.*;
import java.util.*;
import org.jboss.logging.Logger;

/**
 * Extraction strategy that uses the {@link NoOntology} configuration to retrieve the DPP values.
 */
@ApplicationScoped
public class NoOntologyExtractorStrategy implements SearchKeyExtractorStrategy {

    private static final Logger LOGGER = Logger.getLogger(NoOntologyExtractorStrategy.class);

    @Override
    public boolean canHandle(ExtractionStrategyType type) {
        return ExtractionStrategyType.PLAIN_JSON == type;
    }

    @Override
    public Uni<Map<String, Object>> extractSearchKeys(
            ExtractionConfiguration configuration, JsonDocument jsonDocument) {
        Optional<JsonStructure> opStructure = jsonDocument.getJsonContent();
        if (opStructure.isPresent()) {
            JsonStructure json = opStructure.get();
            NoOntology noOntology = configuration.getNoOntology();
            Map<String, FieldType> targetTypes = configuration.searchFieldsAsMap();
            Map<String, NoOntologyFieldSpec> specs = noOntology.getFields();
            Map<String, Object> results = new HashMap<>();
            if (json.getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObject object = json.asJsonObject();
                walkObjectAndCollectValues(object, specs, targetTypes, results);
            } else {
                JsonArray array = json.asJsonArray();
                walkArrayAndCollectValues(array, specs, targetTypes, results);
            }
            return Uni.createFrom().item(results);
        }
        return Uni.createFrom().nullItem();
    }

    private void walkObjectAndCollectValues(
            JsonObject object,
            Map<String, NoOntologyFieldSpec> specs,
            Map<String, FieldType> targetTypes,
            Map<String, Object> results) {
        Set<String> keys = object.keySet();
        for (String k : keys) {
            JsonValue value = object.get(k);
            for (Map.Entry<String, NoOntologyFieldSpec> e : specs.entrySet()) {
                if (matchVariant(k, e.getValue().getVariants())) {
                    if (value.getValueType() != JsonValue.ValueType.ARRAY
                            && value.getValueType() != JsonValue.ValueType.OBJECT) {
                        results.putIfAbsent(
                                e.getKey(),
                                JsonUtils.convertToTargetType(
                                        object.get(k), targetTypes.get(e.getKey())));
                    }
                }
                VariantsWithContext context = e.getValue().getVariantsWithContext();
                if (context != null && matchVariant(k, context.getContext())) {
                    if (value.getValueType() == JsonValue.ValueType.ARRAY)
                        findUnderContext(
                                results, value.asJsonArray(), targetTypes, context, e.getKey());
                    else if (value.getValueType() == JsonValue.ValueType.OBJECT)
                        findUnderContext(
                                results, value.asJsonObject(), targetTypes, context, e.getKey());
                    else
                        warn(
                                LOGGER,
                                () ->
                                        "Skipping a non array and non object json to search in context retrieved with key %s"
                                                .formatted(k));
                }
            }
            if (value.getValueType() == JsonValue.ValueType.OBJECT)
                walkObjectAndCollectValues(value.asJsonObject(), specs, targetTypes, results);
            else if (value.getValueType() == JsonValue.ValueType.ARRAY)
                walkArrayAndCollectValues(value.asJsonArray(), specs, targetTypes, results);
        }
    }

    private void walkArrayAndCollectValues(
            JsonArray jsonArray,
            Map<String, NoOntologyFieldSpec> specs,
            Map<String, FieldType> targetTypes,
            Map<String, Object> results) {
        for (JsonValue v : jsonArray) {
            if (v.getValueType() == JsonValue.ValueType.OBJECT)
                walkObjectAndCollectValues(v.asJsonObject(), specs, targetTypes, results);
        }
    }

    private boolean matchVariant(String key, List<String> variants) {
        String nKey = normalizeKey(key);
        return key != null
                && variants != null
                && variants.stream().map(this::normalizeKey).anyMatch(v -> Objects.equals(v, nKey));
    }

    private String normalizeKey(String key) {
        if (key == null) return null;
        return key.toLowerCase()
                .replaceAll("[_\\-\\s]", "") // remove common JSON separators
                .trim();
    }

    private void findUnderContext(
            Map<String, Object> results,
            JsonObject object,
            Map<String, FieldType> targetTypes,
            VariantsWithContext variantsWithContext,
            String searchFieldName) {
        for (String k : object.keySet()) {
            if (matchVariant(k, variantsWithContext.getField())) {
                results.putIfAbsent(
                        searchFieldName,
                        JsonUtils.convertToTargetType(
                                object.get(k), targetTypes.get(searchFieldName)));
            }
        }
    }

    private void findUnderContext(
            Map<String, Object> results,
            JsonArray array,
            Map<String, FieldType> targetTypes,
            VariantsWithContext variantsWithContext,
            String searchFieldName) {
        for (JsonValue value : array) {
            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                findUnderContext(
                        results,
                        value.asJsonObject(),
                        targetTypes,
                        variantsWithContext,
                        searchFieldName);
            } else {
                warn(
                        LOGGER,
                        () ->
                                "Skipping non object value in array while searching for variants under context");
            }
        }
    }
}
