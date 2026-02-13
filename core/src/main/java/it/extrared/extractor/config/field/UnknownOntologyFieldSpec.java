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
 * Represent a field specification for a configuration concerning DPP compliant with an ontology
 * different from the reference one.
 */
public class UnknownOntologyFieldSpec {

    private List<String> variants;
    private List<String> typeHints;

    /**
     * Get the variants of the field name for the value to be retrieved.
     *
     * @return the variants.
     */
    public List<String> getVariants() {
        return variants;
    }

    /**
     * Sets the variants of the field name for the value to be retrieved.
     *
     * @param variants the variants to be set.
     */
    public void setVariants(List<String> variants) {
        this.variants = variants;
    }

    /**
     * Gets the type hints, i.e. the @type in the JSON-LD object expected to contains propertie to
     * be extracted.
     *
     * @return the type hints.
     */
    public List<String> getTypeHints() {
        return typeHints;
    }

    /**
     * Sets the type hints.
     *
     * @param typeHints the type hints to set.
     */
    public void setTypeHints(List<String> typeHints) {
        this.typeHints = typeHints;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UnknownOntologyFieldSpec that = (UnknownOntologyFieldSpec) o;
        return Objects.equals(variants, that.variants) && Objects.equals(typeHints, that.typeHints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variants, typeHints);
    }

    @Override
    public String toString() {
        return "UnknownOntologyFieldSpec{"
                + "variants="
                + variants
                + ", typeHints="
                + typeHints
                + '}';
    }
}
