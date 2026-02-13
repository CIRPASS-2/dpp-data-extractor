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
package it.extrared.extractor.failures;

/**
 * Represent an extraction failure entry for the corresponding repository. Extraction failures are
 * persisted to allow retrials.
 */
public class ExtractionFailure {

    private Long id;

    private String registryId;

    private Integer retrials;

    public ExtractionFailure() {}

    public ExtractionFailure(String registryId) {
        this.registryId = registryId;
        this.retrials = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistryId() {
        return registryId;
    }

    public void setRegistryId(String registryId) {
        this.registryId = registryId;
    }

    public Integer getRetrials() {
        return retrials;
    }

    public void setRetrials(Integer retrials) {
        this.retrials = retrials;
    }
}
