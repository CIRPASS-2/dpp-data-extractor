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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.SqlConnection;
import it.extrared.extractor.data.SearchData;
import it.extrared.extractor.data.SearchDataRepository;
import it.extrared.extractor.dpp.DPPFetcher;
import it.extrared.extractor.failures.ExtractionFailure;
import it.extrared.extractor.failures.ExtractionFailureRepository;
import it.extrared.extractor.registry.DPPMetadataEntry;
import it.extrared.extractor.registry.DPPMetadataRepository;
import it.extrared.extractor.registry.ExtractionRegistryEntry;
import it.extrared.extractor.registry.ExtractionRegistryRepository;
import it.extrared.extractor.strategies.SearchKeyExtractor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

/** Service class that performs the extraction process. */
@ApplicationScoped
public class ExtractionService {
    @Inject Pool metaPool;

    @Inject
    @ReactiveDataSource("extraction")
    Pool exPool;

    @Inject DPPFetcher dppFetcher;

    @Inject DPPMetadataRepository metadataRepository;

    @Inject ExtractionFailureRepository failureRepository;

    @Inject ExtractionRegistryRepository registryRepository;

    @Inject SearchDataRepository searchDataRepository;

    @Inject SearchKeyExtractor extractor;

    @Inject ExtractorConfig config;

    @Inject ObjectMapper objectMapper;

    private static final Logger LOGGER = Logger.getLogger(ExtractionService.class);

    /**
     * This method retrieves the {@link it.extrared.extractor.registry.ExtractionRegistryEntry},
     * select the metadata using {@link
     * it.extrared.extractor.registry.DPPMetadataRepository#findByDateGtEq(SqlConnection,
     * LocalDateTime)} then for each entry try to fetch the DPP and perform extraction on it. In
     * case of a failure persist the failure details in the {@link
     * it.extrared.extractor.failures.ExtractionFailureRepository}.
     *
     * @return a {@link Uni<Void>}
     */
    public Uni<Void> extract() {
        Uni<LocalDateTime> entryUni =
                exPool.withConnection(c -> registryRepository.get(c))
                        .map(ExtractionRegistryEntry::getProcessedUntil);

        Uni<List<DPPMetadataEntry>> metas =
                entryUni.flatMap(
                        dt ->
                                metaPool.withConnection(
                                        c -> metadataRepository.findByDateGtEq(c, dt)));
        return metas.flatMap(this::extractInternal);
    }

    /**
     * It also retrieves the failures if present from the {@link
     * it.extrared.extractor.failures.ExtractionFailureRepository} and the corresponding DPP
     * metadata to retry the extraction, until the number of max attempts are reached.
     *
     * @return a {@link Uni<Void>}
     */
    public Uni<Void> extractFromFailures() {
        Uni<List<ExtractionFailure>> failures =
                exPool.withConnection(c -> failureRepository.getExtractionFailures(c));
        Uni<List<String>> ids =
                failures.map(l -> l.stream().map(ExtractionFailure::getRegistryId).toList());
        Uni<List<DPPMetadataEntry>> metas =
                ids.flatMap(
                        l -> {
                            if (l == null || l.isEmpty()) {
                                return Uni.createFrom().item(Collections.emptyList());
                            }
                            return metaPool.withConnection(
                                    c -> metadataRepository.findByRegistryIds(c, l));
                        });
        return metas.flatMap(this::extractInternal);
    }

    private LocalDateTime findMaxDateTime(List<DPPMetadataEntry> metas) {
        return metas.stream()
                .max(Comparator.comparing(DPPMetadataEntry::getModifiedAt))
                .map(DPPMetadataEntry::getModifiedAt)
                .orElse(LocalDateTime.MIN);
    }

    private Uni<Void> extractInternal(List<DPPMetadataEntry> metadataL) {
        List<Uni<SearchData>> searches = metadataL.stream().map(this::getSearchData).toList();
        if (searches.isEmpty()) return Uni.createFrom().voidItem();
        Uni<List<SearchData>> combined =
                Uni.combine().all().unis(searches).with(ls -> (List<SearchData>) ls);
        return combined.flatMap(
                l ->
                        exPool.withTransaction(
                                c ->
                                        registryRepository
                                                .updateDateTime(c, findMaxDateTime(metadataL))
                                                .flatMap(
                                                        v ->
                                                                searchDataRepository.batchInsert(
                                                                        c, l))));
    }

    private Uni<SearchData> getSearchData(DPPMetadataEntry metadataEntry) {
        Uni<byte[]> dpp = dppFetcher.fetchDPP(metadataEntry.getLiveURL());
        Uni<Map<String, Object>> result =
                dpp.flatMap(b -> extractor.extractSearchKeys(new ByteArrayInputStream(b)));
        return result.map(m -> toSearchData(metadataEntry, m))
                .onFailure()
                .call(
                        e -> {
                            LOGGER.error(
                                    "Error while trying to extract search data from live url %s"
                                            .formatted(metadataEntry.getLiveURL()));
                            return updateFailure(metadataEntry.getRegistryId());
                        });
    }

    private Uni<Void> updateFailure(String registryId) {
        return exPool.withTransaction(
                c ->
                        failureRepository
                                .createOrIncrease(c, new ExtractionFailure(registryId))
                                .flatMap(
                                        f -> {
                                            if (f.getRetrials() > config.maxRetrials())
                                                return failureRepository.deleteExtractionFailure(
                                                        c, f.getId());
                                            return Uni.createFrom().voidItem();
                                        }));
    }

    private SearchData toSearchData(DPPMetadataEntry metadataEntry, Map<String, Object> result) {
        SearchData searchData = new SearchData();
        searchData.setUpi(metadataEntry.getUpi());
        searchData.setLiveUrl(metadataEntry.getLiveURL());
        JsonNode data = objectMapper.convertValue(result, JsonNode.class);
        searchData.setData(data);
        return searchData;
    }
}
