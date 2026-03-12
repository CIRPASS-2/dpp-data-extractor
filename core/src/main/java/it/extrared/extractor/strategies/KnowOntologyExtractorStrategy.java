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

import static it.extrared.extractor.utils.CommonUtils.*;
import static it.extrared.extractor.utils.JsonUtils.*;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.config.field.FieldType;
import it.extrared.extractor.config.field.KnownOntologyFieldRef;
import it.extrared.extractor.config.field.KnownOntologyFieldSpec;
import it.extrared.extractor.config.field.ParentKnownOntologyFieldRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.*;
import java.util.*;
import org.jboss.logging.Logger;

/**
 * Extraction strategy that uses the {@link it.extrared.extractor.config.KnownOntology}
 * configuration to retrieve DPP data.
 *
 * <p>Builds a graph index (@id → JsonObject) upfront so that IRI-only reference nodes (e.g. {@code
 * {"@id": "http://..."}}) are resolved to their full representation before matching types and
 * extracting values. Without this, any property whose value is a reference to another node in the
 * graph (hasManufacturer, hasProperty, etc.) would never be resolved and its children would never
 * be extracted.
 */
@ApplicationScoped
public class KnowOntologyExtractorStrategy implements SearchKeyExtractorStrategy {

    private static final Logger LOGGER = Logger.getLogger(KnowOntologyExtractorStrategy.class);

    @Override
    public boolean canHandle(ExtractionStrategyType type) {
        return Objects.equals(ExtractionStrategyType.KNOW_ONTOLOGY, type);
    }

    @Override
    public Uni<Map<String, Object>> extractSearchKeys(
            ExtractionConfiguration configuration, JsonDocument jsonDocument) {
        try {
            JsonArray array = JsonLd.expand(jsonDocument).get();
            Map<String, FieldType> types = configuration.searchFieldsAsMap();
            Map<String, Object> results = new HashMap<>();

            // Build a flat index of all named nodes so IRI-only refs can be resolved
            Map<String, JsonObject> graph = buildGraphIndex(array);

            traverseAndPopulate(
                    array, graph, configuration.getKnownOntology().getFields(), types, results);
            return Uni.createFrom().item(results);
        } catch (JsonLdError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds a map of {@code @id → JsonObject} from the top-level array of the expanded JSON-LD
     * document. Used to resolve IRI-only reference nodes.
     */
    private Map<String, JsonObject> buildGraphIndex(JsonArray array) {
        Map<String, JsonObject> index = new HashMap<>();
        for (JsonValue v : array) {
            if (v.getValueType() != JsonValue.ValueType.OBJECT) continue;
            JsonObject obj = v.asJsonObject();
            JsonValue id = obj.get(ID);
            if (id instanceof JsonString js) {
                index.put(js.getString(), obj);
            }
        }
        return index;
    }

    /**
     * Returns true if the object is an IRI-only reference node, i.e. it has only {@code @id} and no
     * other meaningful properties (no {@code @type}, no data).
     */
    private boolean isIriOnly(JsonObject obj) {
        return obj.containsKey(ID)
                && obj.keySet().stream().allMatch(k -> k.equals(ID) || k.equals(TYPE))
                && !obj.containsKey(TYPE);
    }

    /**
     * If the given object is an IRI-only ref, attempts to resolve it from the graph index. Returns
     * the resolved full node, or the original object if not found.
     */
    private JsonObject resolve(JsonObject obj, Map<String, JsonObject> graph) {
        if (!isIriOnly(obj)) return obj;
        JsonValue idVal = obj.get(ID);
        if (idVal instanceof JsonString js) {
            return graph.getOrDefault(js.getString(), obj);
        }
        return obj;
    }

    private void traverseAndPopulate(
            JsonArray array,
            Map<String, JsonObject> graph,
            Map<String, KnownOntologyFieldSpec> fields,
            Map<String, FieldType> types,
            Map<String, Object> result) {
        for (Map.Entry<String, KnownOntologyFieldSpec> e : fields.entrySet()) {
            KnownOntologyFieldSpec spec = e.getValue();
            for (JsonValue value : array) {
                JsonObject o = value.asJsonObject();
                getFromObjectOrWalk(result, o, null, graph, types, e.getKey(), spec.getReference());
            }
        }
    }

    private void getFromObjectOrWalk(
            Map<String, Object> result,
            JsonObject jsonObject,
            String type,
            Map<String, JsonObject> graph,
            Map<String, FieldType> targetTypes,
            String targetName,
            KnownOntologyFieldRef ref) {

        JsonObject resolved = resolve(jsonObject, graph);

        JsonValue atType = resolved.get(TYPE);
        if (!matchType(atType, type)) return;

        Optional<String> opK =
                resolved.keySet().stream()
                        .filter(
                                k ->
                                        k != null
                                                && k.toUpperCase()
                                                        .endsWith(ref.getKey().toUpperCase()))
                        .findFirst();
        if (opK.isEmpty()) return;

        JsonValue jsonValue = resolved.get(opK.get());

        if (jsonValue != null
                && jsonValue.getValueType() != JsonValue.ValueType.NULL
                && ref instanceof ParentKnownOntologyFieldRef pr) {
            KnownOntologyFieldRef childSpec = pr.getChild();
            if (jsonValue.getValueType() == JsonValue.ValueType.OBJECT) {
                getFromObjectOrWalk(
                        result,
                        jsonValue.asJsonObject(),
                        pr.getType(),
                        graph,
                        targetTypes,
                        targetName,
                        childSpec);
            } else if (jsonValue.getValueType() == JsonValue.ValueType.ARRAY) {
                getFromArrayOrWalk(
                        result,
                        jsonValue.asJsonArray(),
                        pr.getType(),
                        graph,
                        targetTypes,
                        targetName,
                        childSpec);
            } else {
                warn(
                        LOGGER,
                        () ->
                                "Retrieved a value by key %s with type %s that is marked as container. However it is neither a JSON object nor a JSON array."
                                        .formatted(pr.getKey(), pr.getType()));
            }
        } else if (jsonValue != null) {
            if (jsonValue.getValueType() == JsonValue.ValueType.ARRAY) {
                JsonArray array = jsonValue.asJsonArray();
                for (JsonValue value : array)
                    tryGetValue(result, value.asJsonObject(), targetTypes, targetName);
            }
        }
    }

    private void getFromArrayOrWalk(
            Map<String, Object> result,
            JsonArray jsonArray,
            String type,
            Map<String, JsonObject> graph,
            Map<String, FieldType> targetTypes,
            String targetName,
            KnownOntologyFieldRef ref) {
        for (JsonValue el : jsonArray) {
            if (el.getValueType() == JsonValue.ValueType.OBJECT) {
                getFromObjectOrWalk(
                        result, el.asJsonObject(), type, graph, targetTypes, targetName, ref);
            }
        }
    }

    private boolean matchType(JsonValue atType, String type) {
        if (type == null) return true;
        if (atType == null || atType.getValueType() == JsonValue.ValueType.NULL) return false;
        if (atType.getValueType() == JsonValue.ValueType.ARRAY) {
            return atType.asJsonArray().stream()
                    .map(v -> ((JsonString) v).getString())
                    .filter(Objects::nonNull)
                    .anyMatch(s -> s.toUpperCase().endsWith(type.toUpperCase()));
        }
        if (atType instanceof JsonString js) {
            return js.getString().toUpperCase().endsWith(type.toUpperCase());
        }
        return false;
    }

    private void tryGetValue(
            Map<String, Object> results,
            JsonObject jsonObject,
            Map<String, FieldType> targetTypes,
            String targetName) {
        if (jsonObject.containsKey(VALUE))
            results.put(
                    targetName,
                    convertToTargetType(jsonObject.get(VALUE), targetTypes.get(targetName)));
        else if (jsonObject.containsKey(ID))
            results.put(
                    targetName,
                    convertToTargetType(jsonObject.get(ID), targetTypes.get(targetName)));
    }
}
