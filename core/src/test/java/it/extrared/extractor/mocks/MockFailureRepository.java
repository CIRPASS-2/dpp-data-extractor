package it.extrared.extractor.mocks;

import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import it.extrared.extractor.failures.ExtractionFailure;
import it.extrared.extractor.failures.ExtractionFailureRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@DefaultBean
@ApplicationScoped
public class MockFailureRepository implements ExtractionFailureRepository {
    private static final String UUID = java.util.UUID.randomUUID().toString();

    @Override
    public Uni<List<ExtractionFailure>> getExtractionFailures(SqlConnection conn) {
        ExtractionFailure failure = new ExtractionFailure();
        failure.setId(1L);
        failure.setRegistryId(UUID);
        return Uni.createFrom().item(List.of(failure));
    }

    @Override
    public Uni<Void> deleteExtractionFailure(SqlConnection conn, Long id) {
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<ExtractionFailure> createOrIncrease(
            SqlConnection conn, ExtractionFailure extractionFailure) {
        ExtractionFailure failure = new ExtractionFailure();
        failure.setId(1L);
        failure.setRegistryId(UUID);
        failure.setRetrials(0);
        return Uni.createFrom().item(failure);
    }
}
