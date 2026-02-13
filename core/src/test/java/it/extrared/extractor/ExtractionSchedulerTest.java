package it.extrared.extractor;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ExtractionSchedulerTest {

    @Inject ExtractionScheduler scheduler;

    @Test
    @RunOnVertxContext
    public void testExtraction(UniAsserter asserter) {
        asserter.assertNull(() -> scheduler.extract());
    }
}
