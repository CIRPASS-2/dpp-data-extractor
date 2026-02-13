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

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/** Class able to retrieve a DPP either as a JSON or as a JSON-LD. */
@ApplicationScoped
public class DPPFetcher {

    @Inject WebClient webClient;

    /**
     * Retrieves a DPP given an url. It accepts all the mime types defined in {@link RDFTypes}. If
     * the content type of the response is in a contentType different from application/json and
     * application/ld+json but still is in a supported RDF mime it converts the payload to a
     * JSON-LD.
     *
     * @param url the url at which the DPP is available.
     * @return the byte[] representing the DPP.
     */
    public Uni<byte[]> fetchDPP(String url) {
        List<String> mimes = RDFTypes.getSupportedContentTypes();
        HttpRequest<Buffer> request = webClient.getAbs(url);
        request.headers().add("Accept", String.join(", ", mimes));
        Uni<HttpResponse<Buffer>> response = request.send();
        return response.map(
                r -> {
                    Buffer b = r.bodyAsBuffer();
                    return normalizeDPP(b, r.getHeader("Content-Type"));
                });
    }

    private byte[] normalizeDPP(Buffer buffer, String contentType) {
        contentType = normalizeContentType(contentType);
        if (buffer == null) {
            return new byte[0];
        }
        byte[] b = buffer.getBytes();
        if (RDFTypes.JSON_LD.mimes().contains(contentType)) {
            return b;
        } else {
            RDFTypes.RDFType rdf = RDFTypes.fromContentType(contentType);
            return convertToJsonLd(rdf.read(new StringReader(new String(b))));
        }
    }

    private byte[] convertToJsonLd(Model model) {
        StringWriter compactWriter = new StringWriter();
        RDFDataMgr.write(compactWriter, model, Lang.JSONLD);
        return compactWriter.toString().getBytes();
    }
}
