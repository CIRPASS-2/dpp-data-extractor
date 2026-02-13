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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;

/** Base class to represent a reference to a field for a DPP compliant to the reference ontology. */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
    @JsonSubTypes.Type(LeafKnownOntologyFieldRef.class),
    @JsonSubTypes.Type(ParentKnownOntologyFieldRef.class)
})
public class KnownOntologyFieldRef {

    private String key;

    /**
     * @return the key of the field to retrieve.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key of the field to retrieve.
     *
     * @param key the key of the field to retrieve.
     */
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        KnownOntologyFieldRef that = (KnownOntologyFieldRef) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    @Override
    public String toString() {
        return "KnownOntologyFieldRef{" + "key='" + key + '\'' + '}';
    }
}
