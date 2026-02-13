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
import static it.extrared.extractor.utils.JsonUtils.convertToTargetType;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.config.field.FieldType;
import it.extrared.extractor.config.field.UnknownOntologyFieldSpec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.util.*;
import org.jboss.logging.Logger;

/**
 * A strategy that uses the {@link it.extrared.extractor.config.UnknownOntology} configuration to
 * extract values from a DPP.
 */
@ApplicationScoped
public class UnknownOntologyExtractorStrategy implements SearchKeyExtractorStrategy {

    private static final Logger LOGGER = Logger.getLogger(UnknownOntologyExtractorStrategy.class);

    @Override
    public boolean canHandle(ExtractionStrategyType type) {
        return ExtractionStrategyType.UNKNOWN_ONTOLOGY == type;
    }

    @Override
    public Uni<Map<String, Object>> extractSearchKeys(
            ExtractionConfiguration configuration, JsonDocument jsonDocument) {
        try {
            JsonArray expanded = JsonLd.expand(jsonDocument).get();
            Map<String, UnknownOntologyFieldSpec> specs =
                    configuration.getUnknownOntology().getFields();
            Map<String, FieldType> targetTypes = configuration.searchFieldsAsMap();
            Map<String, Object> confident = new HashMap<>();
            Map<String, Object> fallback = new HashMap<>();

            walkArray(expanded, specs, confident, fallback, targetTypes);

            fallback.forEach(confident::putIfAbsent);
            return Uni.createFrom().item(confident);
        } catch (JsonLdError e) {
            throw new RuntimeException(e);
        }
    }

    private void walkArray(
            JsonArray array,
            Map<String, UnknownOntologyFieldSpec> specs,
            Map<String, Object> confident,
            Map<String, Object> fallback,
            Map<String, FieldType> targetTypes) {
        for (JsonValue v : array) {
            if (v.getValueType() == JsonValue.ValueType.OBJECT) {
                walkObject(v.asJsonObject(), specs, confident, fallback, targetTypes);
            }
        }
    }

    private void walkObject(
            JsonObject obj,
            Map<String, UnknownOntologyFieldSpec> specs,
            Map<String, Object> confident,
            Map<String, Object> fallback,
            Map<String, FieldType> targetTypes) {

        List<String> localTypes = extractLocalTypes(obj.get("@type"));

        Map<String, String> localKeyCache = new HashMap<>();
        for (String key : obj.keySet()) {
            localKeyCache.put(key, extractLocalName(key));
        }

        for (Map.Entry<String, UnknownOntologyFieldSpec> specEntry : specs.entrySet()) {
            String targetName = specEntry.getKey();
            UnknownOntologyFieldSpec spec = specEntry.getValue();

            if (confident.containsKey(targetName)) continue;

            boolean hasHints = spec.getTypeHints() != null && !spec.getTypeHints().isEmpty();
            boolean typeMatches = typeHintMatches(localTypes, spec.getTypeHints());

            // I've hint and no match so skip
            if (hasHints && !typeMatches) continue;

            for (Map.Entry<String, String> keyEntry : localKeyCache.entrySet()) {
                if (!matchVariant(keyEntry.getValue(), spec.getVariants())) continue;

                JsonValue raw = obj.get(keyEntry.getKey());
                Optional<Object> value = unwrapJsonLdValue(raw, targetTypes.get(targetName));
                if (value.isEmpty()) continue;

                if (hasHints) {
                    confident.put(targetName, value.get());
                } else {
                    fallback.putIfAbsent(targetName, value.get());
                }
                break;
            }
        }

        for (Map.Entry<String, String> keyEntry : localKeyCache.entrySet()) {
            JsonValue v = obj.get(keyEntry.getKey());
            if (v.getValueType() == JsonValue.ValueType.ARRAY) {
                walkArray(v.asJsonArray(), specs, confident, fallback, targetTypes);
            } else if (v.getValueType() == JsonValue.ValueType.OBJECT) {
                walkObject(v.asJsonObject(), specs, confident, fallback, targetTypes);
            }
        }
    }

    private List<String> extractLocalTypes(JsonValue atType) {
        if (atType == null || atType.getValueType() == JsonValue.ValueType.NULL) {
            return Collections.emptyList();
        }
        if (atType.getValueType() == JsonValue.ValueType.ARRAY) {
            return atType.asJsonArray().stream()
                    .filter(v -> v.getValueType() == JsonValue.ValueType.STRING)
                    .map(v -> extractLocalName(((JsonString) v).getString()))
                    .filter(Objects::nonNull)
                    .toList();
        }
        if (atType instanceof JsonString js) {
            String local = extractLocalName(js.getString());
            return local != null ? List.of(local) : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private boolean typeHintMatches(List<String> localTypes, List<String> typeHints) {
        if (typeHints == null || typeHints.isEmpty()) return true;
        if (localTypes.isEmpty()) return false;
        return localTypes.stream()
                .anyMatch(
                        localType ->
                                typeHints.stream()
                                        .filter(Objects::nonNull)
                                        .anyMatch(
                                                hint ->
                                                        localType
                                                                .toUpperCase()
                                                                .endsWith(hint.toUpperCase())));
    }

    private String extractLocalName(String iri) {
        if (iri == null) return null;
        int hash = iri.lastIndexOf('#');
        if (hash >= 0) return iri.substring(hash + 1);
        int slash = iri.lastIndexOf('/');
        if (slash >= 0) return iri.substring(slash + 1);
        return iri;
    }

    private boolean matchVariant(String localKey, List<String> variants) {
        if (localKey == null || variants == null) return false;
        String normalized = normalizeKey(localKey);
        return variants.stream().map(this::normalizeKey).anyMatch(normalized::equals);
    }

    private String normalizeKey(String key) {
        if (key == null) return null;
        return key.toLowerCase().replaceAll("[_\\-\\s]", "").trim();
    }

    private Optional<Object> unwrapJsonLdValue(JsonValue raw, FieldType targetType) {
        if (raw == null || raw.getValueType() == JsonValue.ValueType.NULL) {
            return Optional.empty();
        }
        if (raw.getValueType() == JsonValue.ValueType.ARRAY) {
            for (JsonValue item : raw.asJsonArray()) {
                if (item.getValueType() == JsonValue.ValueType.OBJECT) {
                    JsonObject inner = item.asJsonObject();
                    if (inner.containsKey("@value")) {
                        return Optional.ofNullable(
                                convertToTargetType(inner.get("@value"), targetType));
                    }
                }
            }
            return Optional.empty();
        }
        warn(
                LOGGER,
                () -> "Found non-array value in expanded JSON-LD, attempting direct conversion");
        return Optional.ofNullable(convertToTargetType(raw, targetType));
    }
}
