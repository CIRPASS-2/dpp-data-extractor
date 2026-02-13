package it.extrared.extractor.mocks;

import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.config.loader.ExtractionConfigRepository;
import jakarta.enterprise.context.ApplicationScoped;

@DefaultBean
@ApplicationScoped
public class MockExtractionConfigRepository implements ExtractionConfigRepository {
    @Override
    public Uni<ExtractionConfiguration> getCurrentConfiguration() {
        return Uni.createFrom().nullItem();
    }

    @Override
    public Uni<Void> addConfiguration(ExtractionConfiguration configuration) {
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> removeLastConfiguration() {
        return Uni.createFrom().voidItem();
    }
}
