package it.extrared.extractor.config;

import it.extrared.extractor.config.field.AasFieldSpec;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration to extract values from Asset Administration Shell (AAS) JSON documents. Supports
 * both idShort path navigation and semanticId-based matching.
 */
public class AasOntology {

    private Map<String, AasFieldSpec> fields;

    public Map<String, AasFieldSpec> getFields() {
        return fields;
    }

    public void setFields(Map<String, AasFieldSpec> fields) {
        this.fields = fields;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(fields, ((AasOntology) o).fields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fields);
    }

    @Override
    public String toString() {
        return "AasOntology{fields=" + fields + '}';
    }
}
