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

import it.extrared.extractor.config.field.FieldType;
import java.util.function.Supplier;
import org.jboss.logging.Logger;

/** UUID related utils methods. */
public class CommonUtils {

    private static final Logger LOGGER = Logger.getLogger(CommonUtils.class);

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String INTEGER_REGEX = "^[+-]?\\d+$";
    public static final String DOUBLE_REGEX = "^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$";

    public static void debug(Logger logger, Supplier<String> message) {
        if (logger.isDebugEnabled()) logger.debug(message.get());
    }

    public static void debug(Logger logger, Supplier<String> message, Throwable t) {
        if (logger.isDebugEnabled()) logger.debug(message.get(), t);
    }

    public static void warn(Logger logger, Supplier<String> message) {
        if (logger.isEnabled(Logger.Level.WARN)) logger.warn(message.get());
    }

    public static boolean isInteger(String value) {
        return value.matches(INTEGER_REGEX);
    }

    public static boolean isDouble(String value) {
        return value.matches(DOUBLE_REGEX);
    }

    public static Object convert(FieldType type, Object value) {
        if (value == null) return null;
        if (type == FieldType.DECIMAL || type == FieldType.INTEGER) {
            if (String.class.isAssignableFrom(value.getClass())) {
                String str = (String) value;
                if (isDouble(str)) return Double.parseDouble(str);
                else if (isInteger(str)) return Long.parseLong(str);
                else
                    warn(
                            LOGGER,
                            () ->
                                    "Value %s seems to not be convertible to numeric"
                                            .formatted(value));

            } else if (!Number.class.isAssignableFrom(value.getClass())) {
                warn(
                        LOGGER,
                        () -> "Value %s seems to not be convertible to numeric".formatted(value));
            }
        } else if (type == FieldType.BOOLEAN && !Boolean.class.isAssignableFrom(value.getClass())) {
            if (String.class.isAssignableFrom(value.getClass()))
                return Boolean.parseBoolean((String) value);
            else warn(LOGGER, () -> "Cannot convert value %s to type boolean".formatted(value));
        } else if (type == FieldType.STRING && !String.class.isAssignableFrom(value.getClass()))
            return value.toString();
        return value;
    }

    public static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        if (semicolon > 0) {
            contentType = contentType.substring(0, semicolon);
        }

        return contentType.trim().toLowerCase();
    }
}
