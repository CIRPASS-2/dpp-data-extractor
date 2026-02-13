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
package it.extrared.extractor.config;

import it.extrared.extractor.config.field.KnownOntologyFieldSpec;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration to extract values from DPP compliant with the CIRPASS-2 reference ontology. It
 * defines the paths to retrieve a value using field reference containing information about the JSON
 * object containing a value or directly about the key of a property to retrieve or a combination of
 * the two.
 */
public class KnownOntology {

    private Map<String, KnownOntologyFieldSpec> fields;

    /**
     *
     * @return the key value pairs of the field to extract where the key is target name of the property
     * to be extracted the value is the spec to retrieve the value. The key must be equals to one of the {@link it.extrared.extractor.config.field.SearchField
     * defined in the root node of the {@link ExtractionConfiguration}.
     */
    public Map<String, KnownOntologyFieldSpec> getFields() {
        return fields;
    }

    /**
     * Sets the fields to the configuration.
     *
     * @param fields the fields to be set.
     */
    public void setFields(Map<String, KnownOntologyFieldSpec> fields) {
        this.fields = fields;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        KnownOntology that = (KnownOntology) o;
        return Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fields);
    }

    @Override
    public String toString() {
        return "KnownOntology{" + "fields=" + fields + '}';
    }
}
