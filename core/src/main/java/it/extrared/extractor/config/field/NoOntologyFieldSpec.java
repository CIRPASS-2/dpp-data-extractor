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
 * Represents the specification of a field to retrieve in a configuration matching DPP with not
 * compliant with an ontology i.e. plain JSON DPP.
 */
public class NoOntologyFieldSpec {
    private List<String> variants;
    private VariantsWithContext variantsWithContext;

    /**
     * @return the variants to test to extract a field.
     */
    public List<String> getVariants() {
        return variants;
    }

    /**
     * Sets the variants to test to extract a field.
     *
     * @param variants the variants.
     */
    public void setVariants(List<String> variants) {
        this.variants = variants;
    }

    /**
     * @return retrieve the variants with context, i.e. variants of field expected to be inside a
     *     container JSON object having in turn variants to match.
     */
    public VariantsWithContext getVariantsWithContext() {
        return variantsWithContext;
    }

    /**
     * Sets the variants with context.
     *
     * @param variantsWithContext the variants with context.
     */
    public void setVariantsWithContext(VariantsWithContext variantsWithContext) {
        this.variantsWithContext = variantsWithContext;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NoOntologyFieldSpec that = (NoOntologyFieldSpec) o;
        return Objects.equals(variants, that.variants)
                && Objects.equals(variantsWithContext, that.variantsWithContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variants, variantsWithContext);
    }

    @Override
    public String toString() {
        return "NoOntologyFieldSpec{"
                + "variants="
                + variants
                + ", variantsWithContext="
                + variantsWithContext
                + '}';
    }
}
