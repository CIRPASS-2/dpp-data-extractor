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
 * Represent the specification of a field to retrieve from DPP. Currently it is mere container for a
 * {@link KnownOntologyFieldRef}.
 */
public class KnownOntologyFieldSpec {

    private KnownOntologyFieldRef reference;

    /**
     * @return the field reference
     */
    public KnownOntologyFieldRef getReference() {
        return reference;
    }

    /**
     * Set the reference.
     *
     * @param reference the reference to be set.
     */
    public void setReference(KnownOntologyFieldRef reference) {
        this.reference = reference;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        KnownOntologyFieldSpec that = (KnownOntologyFieldSpec) o;
        return Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(reference);
    }

    @Override
    public String toString() {
        return "KnownOntologyFieldSpec{" + "reference=" + reference + '}';
    }
}
