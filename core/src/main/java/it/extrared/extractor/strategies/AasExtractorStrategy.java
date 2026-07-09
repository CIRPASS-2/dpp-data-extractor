package it.extrared.extractor.strategies;

import static it.extrared.extractor.utils.CommonUtils.warn;

import com.apicatalog.jsonld.document.JsonDocument;
import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.AasOntology;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.config.field.AasFieldSpec;
import it.extrared.extractor.config.field.FieldType;
import it.extrared.extractor.utils.JsonUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.*;
import java.util.*;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

/**
 * Extraction strategy for Asset Administration Shell (AAS) JSON documents.
 *
 * <p>Navigates the {@code submodels[].submodelElements} tree. For each configured field:
 *
 * <ol>
 *   <li>Attempts a full-document scan matching {@code semanticId.keys[].value} (preferred).
 *   <li>Falls back to idShort-path navigation if semanticId is absent or unmatched.
 * </ol>
 *
 * <p>Handles the following AAS modelTypes:
 *
 * <ul>
 *   <li>{@code Property} — scalar value
 *   <li>{@code MultiLanguageProperty} — picks value by {@link AasFieldSpec#getPreferLanguage()},
 *       falls back to first available language
 *   <li>{@code SubmodelElementCollection} / {@code SubmodelElementList} — transparent during
 *       idShort path navigation
 * </ul>
 */
@ApplicationScoped
public class AasExtractorStrategy implements SearchKeyExtractorStrategy {

    private static final Logger LOGGER = Logger.getLogger(AasExtractorStrategy.class);

    private static final String SUBMODELS = "submodels";
    private static final String SUBMODEL_ELEMENTS = "submodelElements";
    private static final String ID_SHORT = "idShort";
    private static final String MODEL_TYPE = "modelType";
    private static final String VALUE = "value";
    private static final String SEMANTIC_ID = "semanticId";
    private static final String KEYS = "keys";
    private static final Pattern IRDI_VERSION_SUFFIX = Pattern.compile("#\\d+$");
    private static final String LANGUAGE = "language";
    private static final String TEXT = "text";

    @Override
    public boolean canHandle(ExtractionStrategyType type) {
        return ExtractionStrategyType.AAS_JSON == type;
    }

    @Override
    public Uni<Map<String, Object>> extractSearchKeys(
            ExtractionConfiguration configuration, JsonDocument jsonDocument) {

        Optional<JsonStructure> opStructure = jsonDocument.getJsonContent();
        if (opStructure.isEmpty()) return Uni.createFrom().nullItem();

        JsonStructure root = opStructure.get();
        if (root.getValueType() != JsonValue.ValueType.OBJECT) {
            warn(LOGGER, () -> "AAS document root is not a JSON object, skipping");
            return Uni.createFrom().item(Collections.emptyMap());
        }

        JsonObject rootObj = root.asJsonObject();
        AasOntology aasOntology = configuration.getAasOntology();
        Map<String, FieldType> targetTypes = configuration.searchFieldsAsMap();
        Map<String, Object> results = new HashMap<>();

        // Collect ALL submodelElements across all submodels into a flat list for semanticId scan
        List<JsonObject> allElements = collectAllElements(rootObj);

        for (Map.Entry<String, AasFieldSpec> entry : aasOntology.getFields().entrySet()) {
            String targetName = entry.getKey();
            AasFieldSpec spec = entry.getValue();
            FieldType targetType = targetTypes.get(targetName);

            Object value = null;

            // 1 — semanticId scan (precise, ontology-stable)
            if (spec.getSemanticId() != null) {
                value = findBySemanticId(allElements, spec, targetType);
            }

            // idShort path navigation (structural fallback)
            if (value == null
                    && spec.getIdShortPath() != null
                    && !spec.getIdShortPath().isEmpty()) {
                value = findByIdShortPath(rootObj, spec, targetType);
            }

            if (value != null) {
                results.put(targetName, value);
            } else {
                warn(LOGGER, () -> "No value found for field: " + targetName);
            }
        }

        return Uni.createFrom().item(results);
    }

    // -------------------------------------------------------------------------
    // Semantic ID scan
    // -------------------------------------------------------------------------

    /**
     * Recursively collects every leaf/intermediate JsonObject in the AAS element tree, so
     * semanticId matching can be done in a single flat pass regardless of nesting depth.
     */
    private List<JsonObject> collectAllElements(JsonObject root) {
        List<JsonObject> acc = new ArrayList<>();
        JsonArray submodels = getArray(root, SUBMODELS);
        if (submodels == null) return acc;
        for (JsonValue sm : submodels) {
            if (sm.getValueType() == JsonValue.ValueType.OBJECT) {
                collectElementsRecursive(sm.asJsonObject(), acc);
            }
        }
        return acc;
    }

    private void collectElementsRecursive(JsonObject node, List<JsonObject> acc) {
        acc.add(node);
        JsonArray children = getArray(node, SUBMODEL_ELEMENTS);
        if (children != null) {
            for (JsonValue child : children) {
                if (child.getValueType() == JsonValue.ValueType.OBJECT) {
                    collectElementsRecursive(child.asJsonObject(), acc);
                }
            }
        }
        // SubmodelElementList stores children directly in "value"
        JsonValue valueNode = node.get(VALUE);
        if (valueNode != null && valueNode.getValueType() == JsonValue.ValueType.ARRAY) {
            for (JsonValue child : valueNode.asJsonArray()) {
                if (child.getValueType() == JsonValue.ValueType.OBJECT) {
                    JsonObject childObj = child.asJsonObject();
                    if (childObj.containsKey(MODEL_TYPE)) { // it's an AAS element, not a lang map
                        collectElementsRecursive(childObj, acc);
                    }
                }
            }
        }
    }

    private Object findBySemanticId(
            List<JsonObject> elements, AasFieldSpec spec, FieldType targetType) {
        for (JsonObject el : elements) {
            if (matchesSemanticId(el, spec.getSemanticId())) {
                return extractValue(el, spec.getPreferLanguage(), targetType);
            }
        }
        return null;
    }

    /**
     * Checks if {@code semanticId.keys[].value} of the element matches the configured semantic ID.
     * Two tolerances let version-less configured targets (e.g. {@code 0173-1#02-AAO677}) match
     * real-world AAS values:
     *
     * <ul>
     *   <li>a leading IRI prefix on the AAS value is tolerated via {@code endsWith};
     *   <li>a trailing eCl@ss/IRDI version suffix on the AAS value (e.g. {@code
     *       0173-1#02-AAO677#002}) is tolerated by also comparing the value with that suffix
     *       stripped — {@code endsWith} alone cannot see past it.
     * </ul>
     */
    private boolean matchesSemanticId(JsonObject element, String targetSemanticId) {
        JsonObject semanticIdObj = getObject(element, SEMANTIC_ID);
        if (semanticIdObj == null) return false;
        JsonArray keys = getArray(semanticIdObj, KEYS);
        if (keys == null) return false;
        for (JsonValue key : keys) {
            if (key.getValueType() != JsonValue.ValueType.OBJECT) continue;
            JsonValue keyValue = key.asJsonObject().get(VALUE);
            if (keyValue instanceof JsonString js) {
                String candidate = js.getString();
                if (matchesTarget(candidate, targetSemanticId)
                        || matchesTarget(withoutVersionSuffix(candidate), targetSemanticId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Exact match (case-insensitive) OR the AAS value ends with the target (IRI-prefix tolerant).
     */
    private boolean matchesTarget(String candidate, String target) {
        return candidate.equalsIgnoreCase(target)
                || candidate.toUpperCase().endsWith(target.toUpperCase());
    }

    /**
     * Strips a trailing IRDI version segment ({@code 0173-1#02-AAO677#002} → {@code
     * 0173-1#02-AAO677}). Applied only when another {@code #} remains, so plain IRIs with a numeric
     * fragment are left untouched.
     */
    private String withoutVersionSuffix(String semanticId) {
        String stripped = IRDI_VERSION_SUFFIX.matcher(semanticId).replaceFirst("");
        return stripped.contains("#") ? stripped : semanticId;
    }

    // -------------------------------------------------------------------------
    // idShort path navigation
    // -------------------------------------------------------------------------

    private Object findByIdShortPath(JsonObject root, AasFieldSpec spec, FieldType targetType) {
        List<String> path = spec.getIdShortPath();

        // Start from submodels array — first segment identifies the submodel (or its idShort)
        JsonArray submodels = getArray(root, SUBMODELS);
        if (submodels == null) return null;

        for (JsonValue sm : submodels) {
            if (sm.getValueType() != JsonValue.ValueType.OBJECT) continue;
            JsonObject smObj = sm.asJsonObject();

            // path[0] matches the submodel idShort; remaining segments navigate submodelElements
            if (path.size() == 1) {
                // The field is directly on the submodel level (unusual but supported)
                if (idShortMatches(smObj, path.getFirst())) {
                    return extractValue(smObj, spec.getPreferLanguage(), targetType);
                }
            } else {
                if (idShortMatches(smObj, path.getFirst())) {
                    JsonObject found = navigatePath(smObj, path.subList(1, path.size()));
                    if (found != null) {
                        return extractValue(found, spec.getPreferLanguage(), targetType);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Recursively descends into {@code submodelElements} following the idShort path segments.
     *
     * @param current current AAS node
     * @param remainingPath remaining idShort segments to match
     * @return the matching leaf JsonObject, or null
     */
    private JsonObject navigatePath(JsonObject current, List<String> remainingPath) {
        if (remainingPath.isEmpty()) return current;

        String nextIdShort = remainingPath.getFirst();
        List<String> rest = remainingPath.subList(1, remainingPath.size());

        JsonArray children = getArray(current, SUBMODEL_ELEMENTS);
        if (children == null) {
            // SubmodelElementList: children are in "value"
            JsonValue valueNode = current.get(VALUE);
            if (valueNode != null && valueNode.getValueType() == JsonValue.ValueType.ARRAY) {
                children = valueNode.asJsonArray();
            }
        }
        if (children == null) return null;

        for (JsonValue child : children) {
            if (child.getValueType() != JsonValue.ValueType.OBJECT) continue;
            JsonObject childObj = child.asJsonObject();
            if (idShortMatches(childObj, nextIdShort)) {
                return rest.isEmpty() ? childObj : navigatePath(childObj, rest);
            }
        }
        return null;
    }

    private boolean idShortMatches(JsonObject obj, String idShort) {
        JsonValue v = obj.get(ID_SHORT);
        return v instanceof JsonString js && js.getString().equalsIgnoreCase(idShort);
    }

    /**
     * Extracts the value from an AAS element node, handling the main modelTypes: {@code Property},
     * {@code MultiLanguageProperty}.
     */
    private Object extractValue(JsonObject element, String preferLanguage, FieldType targetType) {
        String modelType = getString(element, MODEL_TYPE);
        JsonValue rawValue = element.get(VALUE);
        if (rawValue == null || rawValue.getValueType() == JsonValue.ValueType.NULL) return null;

        if ("MultiLanguageProperty".equalsIgnoreCase(modelType)
                && rawValue.getValueType() == JsonValue.ValueType.ARRAY) {
            return extractMultiLangValue(rawValue.asJsonArray(), preferLanguage, targetType);
        }

        if (rawValue.getValueType() == JsonValue.ValueType.STRING
                || rawValue.getValueType() == JsonValue.ValueType.NUMBER
                || rawValue.getValueType() == JsonValue.ValueType.TRUE
                || rawValue.getValueType() == JsonValue.ValueType.FALSE) {
            return JsonUtils.convertToTargetType(rawValue, targetType);
        }

        warn(
                LOGGER,
                () ->
                        "Unsupported value type %s for modelType %s"
                                .formatted(rawValue.getValueType(), modelType));
        return null;
    }

    /**
     * Picks the text value from a {@code MultiLanguageProperty} value array. Tries {@code
     * preferLanguage} first, then falls back to the first available entry.
     */
    private Object extractMultiLangValue(
            JsonArray langArray, String preferLanguage, FieldType targetType) {
        JsonValue fallback = null;
        for (JsonValue item : langArray) {
            if (item.getValueType() != JsonValue.ValueType.OBJECT) continue;
            JsonObject langObj = item.asJsonObject();
            JsonValue textVal = langObj.get(TEXT);
            if (textVal == null) continue;
            String lang = getString(langObj, LANGUAGE);
            if (preferLanguage.equalsIgnoreCase(lang)) {
                return JsonUtils.convertToTargetType(textVal, targetType);
            }
            if (fallback == null) fallback = textVal;
        }
        return fallback != null ? JsonUtils.convertToTargetType(fallback, targetType) : null;
    }

    private JsonArray getArray(JsonObject obj, String key) {
        JsonValue v = obj.get(key);
        return (v != null && v.getValueType() == JsonValue.ValueType.ARRAY)
                ? v.asJsonArray()
                : null;
    }

    private JsonObject getObject(JsonObject obj, String key) {
        JsonValue v = obj.get(key);
        return (v != null && v.getValueType() == JsonValue.ValueType.OBJECT)
                ? v.asJsonObject()
                : null;
    }

    private String getString(JsonObject obj, String key) {
        JsonValue v = obj.get(key);
        return (v instanceof JsonString js) ? js.getString() : null;
    }
}
