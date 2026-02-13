package it.extrared.extractor.datastore.pgsql.extraction;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import it.extrared.extractor.registry.ExtractionRegistryEntry;
import it.extrared.extractor.registry.ExtractionRegistryRepository;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PgSQLExtractionRegistryRepositoryTest {

    @Inject
    @ReactiveDataSource("extraction")
    Pool pool;

    @Inject ExtractionRegistryRepository repository;

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testGet(UniAsserter uniAsserter) {
        Uni<ExtractionRegistryEntry> entry = pool.withTransaction(c -> repository.get(c));
        uniAsserter.assertNotNull(() -> entry);
    }

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testGetAndUpd(UniAsserter uniAsserter) {
        Uni<ExtractionRegistryEntry> entry = pool.withTransaction(c -> repository.get(c));
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        Uni<Void> upd =
                entry.flatMap(
                        e -> pool.withTransaction(c -> repository.updateDateTime(c, yesterday)));
        Uni<LocalDateTime> localDateTimeUni =
                upd.flatMap(v -> pool.withConnection(c -> repository.get(c)))
                        .map(ExtractionRegistryEntry::getProcessedUntil);
        uniAsserter.assertEquals(
                () -> localDateTimeUni.map(ts -> ts.truncatedTo(ChronoUnit.SECONDS)),
                yesterday.truncatedTo(ChronoUnit.SECONDS));
    }
}
