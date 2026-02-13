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
package it.extrared.extractor.datastore.mariadb.extraction;

import static it.extrared.extractor.utils.JsonUtils.toStringJson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.mutiny.sqlclient.Tuple;
import it.extrared.extractor.data.SearchData;
import it.extrared.extractor.data.SearchDataRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/** MariaDB implementation for a {@link SearchDataRepository} */
@ApplicationScoped
public class MariaDBSearchDataRepositoryImpl implements SearchDataRepository {
    private static final String INSERT =
            """
            INSERT INTO dpp_data (upi, live_url, search_data) VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
              live_url = VALUES(live_url),
              search_data = VALUES(search_data)
            """;

    @Inject ObjectMapper mapper;

    @Override
    public Uni<Void> batchInsert(SqlConnection connection, List<SearchData> data) {
        List<Tuple> tuples =
                data.stream()
                        .map(d -> Tuple.of(d.getUpi(), d.getLiveUrl(), toStringJson(d.getData())))
                        .toList();
        return connection.preparedQuery(INSERT).executeBatch(tuples).replaceWithVoid();
    }
}
