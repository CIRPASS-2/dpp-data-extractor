package it.extrared.extractor.datastore.pgsql.extraction;

import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import it.extrared.extractor.ExtractionScheduler;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PgSQLExtractionSchedulerTest {

    @Inject ExtractionScheduler scheduler;

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testExtraction(UniAsserter asserter) {
        asserter.assertNull(() -> scheduler.extract());
    }
}
