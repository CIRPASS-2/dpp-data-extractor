package it.extrared.extractor.datastore.pgsql.extraction;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import it.extrared.extractor.failures.ExtractionFailure;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PgSQLExtractionFailureRepositoryTest {

    @Inject
    @ReactiveDataSource("extraction")
    Pool pool;

    @Inject PgSQLExtractionFailureRepository repository;

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testCreateGetDeleteFailure(UniAsserter uniAsserter) {
        ExtractionFailure extractionFailure = new ExtractionFailure();
        extractionFailure.setRetrials(0);
        String regId = UUID.randomUUID().toString();
        extractionFailure.setRegistryId(regId);
        Uni<ExtractionFailure> failure =
                pool.withTransaction(c -> repository.createOrIncrease(c, extractionFailure));
        AtomicReference<Long> id = new AtomicReference<>();
        uniAsserter.assertNotNull(
                () -> failure.invoke(f -> id.set(f.getId())).map(ExtractionFailure::getId));
        Uni<List<ExtractionFailure>> listUni =
                pool.withConnection(c -> repository.getExtractionFailures(c));
        uniAsserter.assertTrue(
                () -> listUni.map(l -> l.stream().anyMatch(f -> f.getId().equals(id.get()))));
        Uni<Void> delete =
                pool.withTransaction(c -> repository.deleteExtractionFailure(c, id.get()));
        uniAsserter.assertNull(() -> delete);
    }
}
