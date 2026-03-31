package it.extrared.extractor.config.field;

import java.util.List;
import java.util.Objects;

/**
 * Field specification for AAS JSON extraction.
 *
 * <p>Supports two complementary matching strategies:
 *
 * <ul>
 *   <li><b>semanticId</b>: matches against {@code semanticId.keys[].value} — more precise,
 *       ontology-stable across different AAS producers.
 *   <li><b>idShortPath</b>: dot-navigates the submodelElement tree by {@code idShort} — useful when
 *       semanticId is absent or non-standard.
 * </ul>
 *
 * When both are configured, semanticId match takes priority. For {@code MultiLanguageProperty},
 * {@code preferLanguage} drives value selection (default "en").
 */
public class AasFieldSpec {

    /**
     * Ordered list of idShort segments to navigate: e.g. ["Nameplate", "ManufacturerName"]. May be
     * null if semanticId matching is sufficient.
     */
    private List<String> idShortPath;

    /**
     * The semantic ID value to match against {@code semanticId.keys[].value}. May be null if
     * idShortPath matching is sufficient.
     */
    private String semanticId;

    /** Preferred language tag for {@code MultiLanguageProperty} values. Defaults to "en". */
    private String preferLanguage = "en";

    public List<String> getIdShortPath() {
        return idShortPath;
    }

    public void setIdShortPath(List<String> idShortPath) {
        this.idShortPath = idShortPath;
    }

    public String getSemanticId() {
        return semanticId;
    }

    public void setSemanticId(String semanticId) {
        this.semanticId = semanticId;
    }

    public String getPreferLanguage() {
        return preferLanguage;
    }

    public void setPreferLanguage(String preferLanguage) {
        this.preferLanguage = preferLanguage;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AasFieldSpec that = (AasFieldSpec) o;
        return Objects.equals(idShortPath, that.idShortPath)
                && Objects.equals(semanticId, that.semanticId)
                && Objects.equals(preferLanguage, that.preferLanguage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idShortPath, semanticId, preferLanguage);
    }
}
