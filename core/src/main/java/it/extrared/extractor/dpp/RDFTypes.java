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
package it.extrared.extractor.dpp;

import static it.extrared.extractor.utils.CommonUtils.normalizeContentType;

import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/** Class providing constants for supported RDF types. */
public class RDFTypes {

    public record RDFType(List<String> mimes, Lang lang) {

        public Model read(StringReader reader) {
            Model model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, reader, null, lang);
            return model;
        }
    }

    public static final RDFType JSON_LD =
            new RDFType(
                    List.of("application/ld+json", "application/json", "text/json"), Lang.JSONLD);

    public static final RDFType RDF_XML =
            new RDFType(List.of("application/rdf+xml", "application/xml", "text/xml"), Lang.RDFXML);

    public static final RDFType TURTLE =
            new RDFType(List.of("application/x+turtle", "text/turtle"), Lang.TURTLE);

    public static final RDFType NTRIPLES =
            new RDFType(List.of("application/n-triples", "text/plain"), Lang.NTRIPLES);

    public static final RDFType N3 = new RDFType(List.of("text/n3", "text/rdf+n3"), Lang.N3);

    public static final RDFType NQUADS = new RDFType(List.of("application/n-quads"), Lang.NQUADS);

    public static final RDFType RDF_JSON =
            new RDFType(List.of("application/rdf+json"), Lang.RDFJSON);

    public static final RDFType TRIG =
            new RDFType(List.of("application/trig", "application/x-trig"), Lang.TRIG);

    /**
     * Returns a RDF type from the specified content type.
     *
     * @param contentType the contentType.
     * @return the RDFType.
     */
    public static RDFType fromContentType(String contentType) {
        contentType = normalizeContentType(contentType);
        if (JSON_LD.mimes().contains(contentType)) return JSON_LD;
        else if (RDF_XML.mimes().contains(contentType)) return RDF_XML;
        else if (TURTLE.mimes().contains(contentType)) return TURTLE;
        else if (RDF_JSON.mimes().contains(contentType)) return RDF_JSON;
        else if (NTRIPLES.mimes().contains(contentType)) return NTRIPLES;
        else if (N3.mimes().contains(contentType)) return N3;
        else if (NQUADS.mimes().contains(contentType)) return NQUADS;
        else if (TRIG.mimes().contains(contentType)) return TRIG;
        else
            throw new UnsupportedOperationException(
                    "Unsupported content type %s".formatted(contentType));
    }

    private static Stream<String> getSupportedContentTypesStream() {
        return Stream.of(
                        JSON_LD.mimes(),
                        RDF_XML.mimes(),
                        TURTLE.mimes(),
                        NTRIPLES.mimes(),
                        N3.mimes(),
                        NQUADS.mimes(),
                        TRIG.mimes(),
                        RDF_JSON.mimes())
                .flatMap(Collection::stream);
    }

    /**
     * @return the whole list of supported mime types by the RDF types.
     */
    public static List<String> getSupportedContentTypes() {
        return getSupportedContentTypesStream().toList();
    }
}
