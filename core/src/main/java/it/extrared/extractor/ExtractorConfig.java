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
package it.extrared.extractor;

import io.quarkus.runtime.util.StringUtil;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import it.extrared.extractor.security.Roles;
import it.extrared.extractor.utils.MultiMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.spi.Converter;

/** Extractor service configuration interface. */
@ConfigMapping(prefix = "extractor")
public interface ExtractorConfig {

    /**
     * @return the field name of the unique product identifier property in the metadata JSON.
     *     Default is upi.
     */
    @WithDefault("upi")
    String upiFieldName();

    /**
     * @return the field name of the live URL property in the metadata JSON. Default is liveURL.
     */
    @WithDefault("liveURL")
    String liveURLFieldName();

    /**
     * @return the field name of the granularity level property in the JSON. Default is
     *     granularityLevel
     */
    @WithDefault("granularityLevel")
    String granularityFieldName();

    /**
     * @return the value of the granularity level field when it stands for model level data. Default
     *     is MODEL
     */
    @WithDefault("MODEL")
    String granularityLevelModel();

    /**
     * @return the location (URI,relative file path or HTTP URL) of a JSON Extraction configuration
     *     to be loaded.
     */
    Optional<String> extractionConfigLocation();

    @WithDefault("extraction.json")
    String config();

    @WithDefault("3")
    Integer maxRetrials();

    @WithConverter(RolesMappingsConverter.class)
    @WithDefault("admin:admin,eo:eo,eu:eu")
    MultiMap<String, String> rolesMappings();

    Dpp dpp();

    interface Dpp {
        ReferenceOntology referenceOntology();

        interface ReferenceOntology {
            @WithConverter(ListStringConverter.class)
            Optional<List<String>> contexts();

            @WithConverter(ListStringConverter.class)
            Optional<List<String>> vocabularies();
        }
    }

    class RolesMappingsConverter implements Converter<MultiMap<String, String>> {

        @Override
        public MultiMap<String, String> convert(String s)
                throws IllegalArgumentException, NullPointerException {
            MultiMap<String, String> map = new MultiMap<>();
            if (StringUtil.isNullOrEmpty(s)) return map;
            String[] mappings = s.split(",");
            Stream.of(mappings)
                    .map(m -> m.split(":"))
                    .filter(arr -> arr.length >= 2)
                    // validates on roles enum
                    .peek(arr -> Roles.valueOf(arr[1].toUpperCase()))
                    .forEach(arr -> map.add(arr[0], arr[1].toUpperCase()));
            return map;
        }
    }

    class ListStringConverter implements Converter<List<String>> {

        @Override
        public List<String> convert(String s)
                throws IllegalArgumentException, NullPointerException {
            if (!StringUtil.isNullOrEmpty(s)) return Arrays.asList(s.split(","));
            return Collections.emptyList();
        }
    }
}
