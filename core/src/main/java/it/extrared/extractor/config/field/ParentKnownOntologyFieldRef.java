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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Represents a parent field ref for a known ontology configuration, i.e. the ref to a container of
 * the property with the actual value.
 */
public class ParentKnownOntologyFieldRef extends KnownOntologyFieldRef {

    @JsonProperty("@type")
    private String type;

    private KnownOntologyFieldRef child;

    /**
     * @return type for JSON polymorphism
     */
    public String getType() {
        return type;
    }

    /**
     * Set the type for JSON polymorphism
     *
     * @param type the type value.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the child ref of this property ref.
     *
     * @return the child ref.
     */
    public KnownOntologyFieldRef getChild() {
        return child;
    }

    /**
     * Sets the child ref of this property ref.
     *
     * @param child the child ref.
     */
    public void setChild(KnownOntologyFieldRef child) {
        this.child = child;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ParentKnownOntologyFieldRef that = (ParentKnownOntologyFieldRef) o;
        return Objects.equals(type, that.type) && Objects.equals(child, that.child);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, child);
    }

    @Override
    public String toString() {
        return "ParentKnownOntologyFieldRef{" + "type='" + type + '\'' + ", child=" + child + '}';
    }
}
