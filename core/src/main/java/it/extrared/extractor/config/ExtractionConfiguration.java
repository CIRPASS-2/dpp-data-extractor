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

import it.extrared.extractor.config.field.FieldType;
import it.extrared.extractor.config.field.SearchField;
import it.extrared.extractor.exceptions.InvalidExtractionConfigException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A representation of a configuration to extract information from DPPs. It covers three main type
 * of DPP: - JSON-LD DPP compliant with the CIRPASS 2 reference ontology. - JSON-LD DPP
 * non-compliant with the reference ontology. - Plain JSON DPP.
 */
public class ExtractionConfiguration {

    private List<SearchField> searchFields;

    private KnownOntology knownOntology;

    private NoOntology noOntology;

    private UnknownOntology unknownOntology;

    /**
     * @return the Known Ontology configuration.
     */
    public KnownOntology getKnownOntology() {
        return knownOntology;
    }

    /**
     * Sets the Known Ontology configuration.
     *
     * @param knownOntology the Known Ontology configuration.
     */
    public void setKnownOntology(KnownOntology knownOntology) {
        this.knownOntology = knownOntology;
    }

    /**
     * @return the No Ontology configuration
     */
    public NoOntology getNoOntology() {
        return noOntology;
    }

    /**
     * Sets the No Ontology configuration
     *
     * @param noOntology the No Ontology configuration
     */
    public void setNoOntology(NoOntology noOntology) {
        this.noOntology = noOntology;
    }

    /**
     * @return the Unknown Ontology configuration.
     */
    public UnknownOntology getUnknownOntology() {
        return unknownOntology;
    }

    /**
     * Sets the Unknown Ontology configuration.
     *
     * @param unknownOntology the Unknown Ontology configuration.
     */
    public void setUnknownOntology(UnknownOntology unknownOntology) {
        this.unknownOntology = unknownOntology;
    }

    /**
     * @return the list of configured search fields.
     */
    public List<SearchField> getSearchFields() {
        return searchFields;
    }

    /**
     * Sets the list of configured search fields.
     *
     * @param searchFields the list of configured search fields.
     */
    public void setSearchFields(List<SearchField> searchFields) {
        this.searchFields = searchFields;
    }

    /**
     * Converts the list of search fields to map where the key is the field name and the value is
     * the target type.
     *
     * @return the search fields as map.
     */
    public Map<String, FieldType> searchFieldsAsMap() {
        return searchFields.stream()
                .collect(Collectors.toMap(SearchField::getFieldName, SearchField::getTargetType));
    }

    /**
     * Validate that the object graph has the required sub configuration nodes and that each field
     * spec of each subconfiguration is present among the search fields array.
     */
    public void validate() {
        Map<String, FieldType> targetTypes = searchFieldsAsMap();
        if (targetTypes.isEmpty())
            throw new InvalidExtractionConfigException(
                    "No target fields specified. The configuration is invalid");
        KnownOntology knownOntology = getKnownOntology();
        UnknownOntology unknownOntology = getUnknownOntology();
        NoOntology noOntology = getNoOntology();
        if (knownOntology == null)
            throw new InvalidExtractionConfigException(
                    "Known Ontology configuration must be provided.");
        if (noOntology == null)
            throw new InvalidExtractionConfigException(
                    "No Ontology configuration must be provided.");
        if (unknownOntology == null)
            throw new InvalidExtractionConfigException(
                    "Unknown Ontology configuration must be provided.");

        checkSpecFieldNames(KnownOntology.class, knownOntology.getFields().keySet(), targetTypes);
        checkSpecFieldNames(
                UnknownOntology.class, unknownOntology.getFields().keySet(), targetTypes);
        checkSpecFieldNames(NoOntology.class, noOntology.getFields().keySet(), targetTypes);
    }

    private void checkSpecFieldNames(
            Class<?> subConfigType, Set<String> specKeys, Map<String, FieldType> targetTypes) {
        for (String k : specKeys)
            if (!targetTypes.containsKey(k))
                throw new InvalidExtractionConfigException(
                        "Config for %s is invalid: %s is not among the listed searchFields"
                                .formatted(subConfigType.getSimpleName(), k));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ExtractionConfiguration that = (ExtractionConfiguration) o;
        return Objects.equals(searchFields, that.searchFields)
                && Objects.equals(knownOntology, that.knownOntology)
                && Objects.equals(noOntology, that.noOntology)
                && Objects.equals(unknownOntology, that.unknownOntology);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchFields, knownOntology, noOntology, unknownOntology);
    }

    @Override
    public String toString() {
        return "ExtractionConfiguration{"
                + "searchFields="
                + searchFields
                + ", knownOntology="
                + knownOntology
                + ", noOntology="
                + noOntology
                + ", unknownOntology="
                + unknownOntology
                + '}';
    }
}
