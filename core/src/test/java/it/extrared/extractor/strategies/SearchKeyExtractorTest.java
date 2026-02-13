package it.extrared.extractor.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfigCache;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SearchKeyExtractorTest {

    @Inject SearchKeyExtractor searchKeyExtractor;
    @Inject ExtractionConfigCache cache;

    @BeforeEach
    public void before() {
        cache.invalidate();
    }

    @Test
    @RunOnVertxContext
    public void extractFromJson(UniAsserter uniAsserter) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/example-dpp/dpp.json")) {
            Uni<Map<String, Object>> result = searchKeyExtractor.extractSearchKeys(is);
            uniAsserter.assertThat(
                    () -> result,
                    r -> {
                        assertEquals(4, r.size());
                        assertEquals("EcoBattery Ultra 5000mAh", r.get("name"));
                        assertEquals(12.4, ((Number) r.get("total_kg_co2")).doubleValue());
                        assertEquals(45d, ((Number) r.get("weight")).doubleValue());
                        assertEquals("batch", r.get("granularity"));
                    });
        }
    }

    @Test
    @RunOnVertxContext
    public void extractFromJsonLd(UniAsserter uniAsserter) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/example-dpp/dpp-ld.json")) {
            Uni<Map<String, Object>> result = searchKeyExtractor.extractSearchKeys(is);
            Consumer<Map<String, Object>> assertions =
                    r -> {
                        assertEquals("product", r.get("granularity"));
                        assertEquals(45.8, r.get("carbonFootprint"));
                        assertEquals(189.5, r.get("weight"));
                        assertEquals("EcoPhone X Pro", r.get("productName"));
                    };
            uniAsserter.assertThat(() -> result, assertions);
        }
    }

    @Test
    @RunOnVertxContext
    public void extractFromUnknownJsonLd(UniAsserter uniAsserter) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/example-dpp/dpp-unknown-ld.json")) {
            Uni<Map<String, Object>> result = searchKeyExtractor.extractSearchKeys(is);
            Consumer<Map<String, Object>> assertions =
                    r -> {
                        assertEquals("model", r.get("granularity"));
                        assertEquals(45.8, r.get("carbonFootprint"));
                        assertEquals(189.5, r.get("weight"));
                        assertEquals("EcoPhone X Pro", r.get("productName"));
                    };
            uniAsserter.assertThat(() -> result, assertions);
        }
    }
}
