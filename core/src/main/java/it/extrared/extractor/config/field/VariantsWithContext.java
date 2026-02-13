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

import java.util.List;
import java.util.Objects;

/**
 * Represent a set of variants with context i.e. variants that should be tested for extraction only
 * if are found inside a JSON object with a key matching the context variants.
 */
public class VariantsWithContext {

    private List<String> context;
    private List<String> field;

    /**
     * @return the list of context variants.
     */
    public List<String> getContext() {
        return context;
    }

    /**
     * Sets the context variants.
     *
     * @param context the context variants.
     */
    public void setContext(List<String> context) {
        this.context = context;
    }

    /**
     * @return the field variants.
     */
    public List<String> getField() {
        return field;
    }

    /**
     * Set the field variants.
     *
     * @param fields the fields variants.
     */
    public void setField(List<String> fields) {
        this.field = fields;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VariantsWithContext that = (VariantsWithContext) o;
        return Objects.equals(context, that.context) && Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, field);
    }

    @Override
    public String toString() {
        return "VariantsWithContext{" + "context=" + context + ", field=" + field + '}';
    }
}
