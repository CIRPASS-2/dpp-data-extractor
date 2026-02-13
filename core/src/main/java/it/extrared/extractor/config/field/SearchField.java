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
package it.extrared.extractor.config.field;

import java.util.Objects;

/**
 * Represents a search field i.e. a field to be extracted from a DPP to be made available in the
 * search db.
 */
public class SearchField {
    private String fieldName;
    private FieldType targetType;
    private String dependsOn;

    /**
     * @return the field name, i.e. the name that the value will have once extracted.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Sets the field name.
     *
     * @param fieldName the field name.
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Get the name of the field to which this field depends on. Depending means that the field has
     * not an autonomous meaning if not provided together with the field it depends on (for instance
     * field about the unit of measures of value depends on that value field).
     *
     * @return
     */
    public String getDependsOn() {
        return dependsOn;
    }

    /**
     * Sets the depends on value.
     *
     * @param dependsOn
     */
    public void setDependsOn(String dependsOn) {
        this.dependsOn = dependsOn;
    }

    /**
     * Get the target type of the field i.e. the type to which the field value should be converted
     * once extracted.
     *
     * @return the target type.
     */
    public FieldType getTargetType() {
        return targetType;
    }

    /**
     * Sets the target type.
     *
     * @param targetType the target type.
     */
    public void setTargetType(FieldType targetType) {
        this.targetType = targetType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SearchField that = (SearchField) o;
        return Objects.equals(fieldName, that.fieldName)
                && targetType == that.targetType
                && Objects.equals(dependsOn, that.dependsOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, targetType, dependsOn);
    }

    @Override
    public String toString() {
        return "SearchField{"
                + "fieldName='"
                + fieldName
                + '\''
                + ", targetType="
                + targetType
                + ", dependsOn='"
                + dependsOn
                + '\''
                + '}';
    }
}
