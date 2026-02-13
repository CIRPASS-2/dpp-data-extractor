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

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import java.util.List;

/**
 * Base interface for a {@link SearchDataRepository} allowing operation over the search data
 * storage.
 */
public interface SearchDataRepository {

    /**
     * Performs a batch insert of the {@link SearchData}
     *
     * @param connection the {@link SqlConnection}
     * @param data the list of search data to persist.
     * @return a {@link Uni<Void>}
     */
    Uni<Void> batchInsert(SqlConnection connection, List<SearchData> data);
}
