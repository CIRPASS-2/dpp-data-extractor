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
package it.extrared.extractor.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.sqlclient.Row;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.registry.DPPMetadataEntry;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Function;

/** Some useful methods for code using Reactive SQL client. */
public class SQLClientUtils {

    /**
     * Gets a JSON value from the iterator argument if any or returns null.
     *
     * @param objectMapper the jackson object mapper
     * @param rs the result set.
     * @return a {@link JsonNode} representing the JSON value.
     */
    public static ExtractionConfiguration getExtractionConfig(
            ObjectMapper objectMapper, Iterator<byte[]> rs) throws IOException {
        if (rs.hasNext()) {
            byte[] next = rs.next();
            if (next != null) return objectMapper.readValue(next, ExtractionConfiguration.class);
        }
        return null;
    }

    public static Function<Row, DPPMetadataEntry> rowToMetadata(
            String liveUrlFieldName, String upiFieldName) {
        return Unchecked.function(
                r -> {
                    DPPMetadataEntry dppMetadataEntry = new DPPMetadataEntry();
                    dppMetadataEntry.setRegistryId(r.getString("registry_id"));
                    dppMetadataEntry.setModifiedAt(r.getLocalDateTime("modified_at"));
                    dppMetadataEntry.setUpi(r.getString(upiFieldName.toLowerCase()));
                    dppMetadataEntry.setLiveURL(r.getString(liveUrlFieldName.toLowerCase()));
                    return dppMetadataEntry;
                });
    }
}
