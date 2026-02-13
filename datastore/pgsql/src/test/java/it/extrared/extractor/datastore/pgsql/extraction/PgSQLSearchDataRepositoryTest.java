package it.extrared.extractor.datastore.pgsql.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import it.extrared.extractor.data.SearchData;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PgSQLSearchDataRepositoryTest {

    @Inject PgSQLSearchDataRepositoryImpl repository;
    @Inject ObjectMapper objectMapper;

    @Inject
    @ReactiveDataSource("extraction")
    Pool pool;

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testBatchInsert(UniAsserter uniAsserter) {
        List<SearchData> dataList = List.of(createData(1), createData(2));
        Uni<Void> insert = pool.withTransaction(c -> repository.batchInsert(c, dataList));
        uniAsserter.assertNull(() -> insert);
    }

    private SearchData createData(int index) {
        SearchData searchData = new SearchData();
        searchData.setUpi("%s23456789".formatted(index));
        searchData.setLiveUrl("http://my-service/%s".formatted(index));
        ObjectNode on = objectMapper.createObjectNode();
        on.set("productName", objectMapper.getNodeFactory().textNode("prod%s".formatted(index)));
        on.set("weight", objectMapper.getNodeFactory().numberNode(10 * index));
        on.set("carbonFootprint", objectMapper.getNodeFactory().numberNode(12.3 * index));
        searchData.setData(on);
        return searchData;
    }
}
