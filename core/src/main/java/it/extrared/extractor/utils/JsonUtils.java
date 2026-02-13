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
package it.extrared.extractor.utils;

import static it.extrared.extractor.utils.CommonUtils.convert;
import static it.extrared.extractor.utils.CommonUtils.warn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.config.field.FieldType;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.jboss.logging.Logger;

/** Some useful method to handle JSON data. */
public class JsonUtils {

    public static final String ID = "@id";
    public static final String TYPE = "@type";
    public static final String VALUE = "@value";

    private static final Logger LOGGER = Logger.getLogger(JsonUtils.class);

    public static Map<String, Object> toMap(JsonNode node) {
        return objectMapper().convertValue(node, new TypeReference<Map<String, Object>>() {});
    }

    public static Map<String, Object> toMap(ExtractionConfiguration configuration) {
        return objectMapper()
                .convertValue(configuration, new TypeReference<Map<String, Object>>() {});
    }

    public static boolean nodeIsNotNull(JsonNode node) {
        return node != null && !node.isNull() && !node.isMissingNode();
    }

    public static JsonObject toVertxJson(ExtractionConfiguration configuration) {
        return new JsonObject(toMap(configuration));
    }

    public static JsonObject toVertxJson(JsonNode node) {
        return new JsonObject(toMap(node));
    }

    public static JsonNode fromVertxJson(JsonObject object) {
        return objectMapper().convertValue(object.getMap(), JsonNode.class);
    }

    public static String toStringJson(JsonNode node) {
        if (node == null) return null;
        try {
            return objectMapper().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectMapper objectMapper() {
        return CDI.current().select(ObjectMapper.class).get();
    }

    public static <T> T loadClasspathJsonAs(String fileName, Class<T> clazz) throws IOException {
        try (InputStream is =
                JsonUtils.class.getResourceAsStream("/extraction/%s".formatted(fileName))) {
            return objectMapper().readValue(is, clazz);
        }
    }

    public static Object convertToTargetType(JsonValue value, FieldType targetType) {
        Object o = null;
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) return null;
        if (value.getValueType() == JsonValue.ValueType.OBJECT) {
            jakarta.json.JsonObject jo = value.asJsonObject();
            if (jo.containsKey(VALUE)) value = jo.get(VALUE);
        }
        if (value.getValueType() == JsonValue.ValueType.STRING)
            o = ((JsonString) value).getString();
        else if (value.getValueType() == JsonValue.ValueType.NUMBER)
            o = ((JsonNumber) value).numberValue();
        else if (value.getValueType() == JsonValue.ValueType.TRUE) o = true;
        else if (value.getValueType() == JsonValue.ValueType.FALSE) o = false;
        if (o == null) {
            warn(LOGGER, () -> "Expected a value but got either a json object or a json array");
        }
        return convert(targetType, o);
    }

    public static String extractNamespace(String uri) {
        if (uri == null) return null;

        int hashIndex = uri.lastIndexOf('#');
        if (hashIndex > 0) {
            return uri.substring(0, hashIndex + 1);
        }

        int slashIndex = uri.lastIndexOf('/');
        if (slashIndex > 0) {
            return uri.substring(0, slashIndex + 1);
        }

        return uri;
    }
}
