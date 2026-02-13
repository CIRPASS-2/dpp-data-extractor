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

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Scheduler that periodically starts the extraction process. */
@ApplicationScoped
public class ExtractionScheduler {

    @Inject ExtractionService extractionService;

    /**
     * Perform the extraction using the {@link ExtractionService} performing first the normal
     * extraction process then reattempting to recover previous failures if any.
     *
     * @return a {@link Uni<Void>}
     */
    @Scheduled(every = "${start.extractor.every:5s}")
    public Uni<Void> extract() {
        return extractionService.extract().flatMap(v -> extractionService.extractFromFailures());
    }
}
