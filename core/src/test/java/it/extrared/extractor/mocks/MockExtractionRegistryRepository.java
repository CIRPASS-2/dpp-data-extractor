package it.extrared.extractor.mocks;

import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import it.extrared.extractor.registry.ExtractionRegistryEntry;
import it.extrared.extractor.registry.ExtractionRegistryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@DefaultBean
@ApplicationScoped
public class MockExtractionRegistryRepository implements ExtractionRegistryRepository {
    @Override
    public Uni<ExtractionRegistryEntry> get(SqlConnection conn) {
        ExtractionRegistryEntry entry = new ExtractionRegistryEntry();
        entry.setProcessedUntil(LocalDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIN));
        entry.setId(1L);
        return Uni.createFrom().item(entry);
    }

    @Override
    public Uni<Void> updateDateTime(SqlConnection conn, LocalDateTime dateTime) {
        return Uni.createFrom().voidItem();
    }
}
