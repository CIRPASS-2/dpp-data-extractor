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
package it.extrared.extractor.data;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represent a SearchData entry for the persistence layer where the extracted data are persisted.
 * These data can then be used by to enable search capabilities for DPP present in the decentralized
 * repository.
 */
public class SearchData {

    private Long id;

    private String upi;

    private String liveUrl;

    private JsonNode data;

    /**
     * @return unique identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier.
     *
     * @param id the unique identifier.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the unique product identifier.
     */
    public String getUpi() {
        return upi;
    }

    /**
     * Sets the unique product identifier.
     *
     * @param upi the unique product identifier.
     */
    public void setUpi(String upi) {
        this.upi = upi;
    }

    /**
     * @return the live Url.
     */
    public String getLiveUrl() {
        return liveUrl;
    }

    /**
     * Sets the live Url.
     *
     * @param liveUrl the live Url.
     */
    public void setLiveUrl(String liveUrl) {
        this.liveUrl = liveUrl;
    }

    /**
     * @return the data corresponding to the values extracted from the DPP.
     */
    public JsonNode getData() {
        return data;
    }

    /**
     * Sets the DPP data extracted.
     *
     * @param data the DPP data.
     */
    public void setData(JsonNode data) {
        this.data = data;
    }
}
