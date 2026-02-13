package it.extrared.extractor.mocks;

import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import it.extrared.extractor.data.SearchData;
import it.extrared.extractor.data.SearchDataRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@DefaultBean
@ApplicationScoped
public class MockSearchDataRepository implements SearchDataRepository {
    @Override
    public Uni<Void> batchInsert(SqlConnection connection, List<SearchData> data) {
        return Uni.createFrom().voidItem();
    }
}
