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
 * Field reference representing the key of a leaf in a JSON-LD graph i.e. the key of the json value
 * to be retrieved or of the container containing an @value field.
 */
public class LeafKnownOntologyFieldRef extends KnownOntologyFieldRef {

    private FieldType nativeType;

    /**
     * @return the native type (i.e. the type of the value in the JSON-LD).
     */
    public FieldType getNativeType() {
        return nativeType;
    }

    /**
     * Sets the native type of the fied.
     *
     * @param nativeType the native type.
     */
    public void setNativeType(FieldType nativeType) {
        this.nativeType = nativeType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LeafKnownOntologyFieldRef that = (LeafKnownOntologyFieldRef) o;
        return nativeType == that.nativeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nativeType);
    }

    @Override
    public String toString() {
        return "LeafKnownOntologyFieldRef{" + "nativeType=" + nativeType + '}';
    }
}
