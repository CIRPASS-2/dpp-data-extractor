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
package it.extrared.extractor.datastore.pgsql.extraction;

import static it.extrared.extractor.utils.JsonUtils.toVertxJson;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.mutiny.sqlclient.Tuple;
import it.extrared.extractor.data.SearchData;
import it.extrared.extractor.data.SearchDataRepository;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/** PgSQL implementation for a {@link SearchDataRepository}. */
@Priority(1)
@ApplicationScoped
public class PgSQLSearchDataRepositoryImpl implements SearchDataRepository {

    private static final String INSERT =
            """
            INSERT INTO dpp_data (upi, live_url, search_data)
            VALUES ($1, $2, $3)
            ON CONFLICT (upi)
            DO UPDATE SET
                live_url=EXCLUDED.live_url,
                search_data = EXCLUDED.search_data
            """;

    @Override
    public Uni<Void> batchInsert(SqlConnection connection, List<SearchData> data) {
        List<Tuple> tuples =
                data.stream()
                        .map(d -> Tuple.of(d.getUpi(), d.getLiveUrl(), toVertxJson(d.getData())))
                        .toList();
        return connection.preparedQuery(INSERT).executeBatch(tuples).replaceWithVoid();
    }
}
