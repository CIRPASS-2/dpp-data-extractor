package it.extrared.extractor.mocks;

import static it.extrared.extractor.TestSupport.readResourceAsBytes;
import static org.mockito.ArgumentMatchers.eq;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.sqlclient.Pool;
import it.extrared.extractor.strategies.ExtractionStrategyType;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import java.io.IOException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@ApplicationScoped
public class MockProducers {

    @Produces
    @IfBuildProperty(name = "test.mock.pool", stringValue = "true", enableIfMissing = true)
    @ApplicationScoped
    public Pool metaPool() {
        return new MockPool();
    }

    @Produces
    @IfBuildProperty(name = "test.mock.pool", stringValue = "true", enableIfMissing = true)
    @ReactiveDataSource("extraction")
    @ApplicationScoped
    public Pool extractionPool() {
        return new MockPool();
    }

    @Produces
    @Alternative
    @Priority(1)
    @ApplicationScoped
    public WebClient webClient() throws IOException {
        WebClient webClient = Mockito.mock(WebClient.class);
        Mockito.doReturn(mockRequest(ExtractionStrategyType.PLAIN_JSON))
                .when(webClient)
                .getAbs(eq("http://localhost:8080/dpp1"));
        Mockito.doReturn(mockRequest(ExtractionStrategyType.KNOW_ONTOLOGY))
                .when(webClient)
                .getAbs(eq("http://localhost:8080/dpp2"));
        Mockito.doReturn(mockRequest(ExtractionStrategyType.UNKNOWN_ONTOLOGY))
                .when(webClient)
                .getAbs(eq("http://localhost:8080/dpp3"));
        return webClient;
    }

    private HttpRequest<Buffer> mockRequest(ExtractionStrategyType type) throws IOException {
        HttpRequest<Buffer> request = Mockito.mock(HttpRequest.class);
        Mockito.doReturn(MultiMap.caseInsensitiveMultiMap()).when(request).headers();
        Mockito.doReturn(Uni.createFrom().item(mockResponse(type))).when(request).send();
        return request;
    }

    private HttpResponse<Buffer> mockResponse(ExtractionStrategyType type) throws IOException {
        byte[] bytes;
        switch (type) {
            case PLAIN_JSON -> bytes = readResourceAsBytes("/example-dpp/dpp.json");
            case KNOW_ONTOLOGY -> bytes = readResourceAsBytes("/example-dpp/dpp-ld.json");
            default -> bytes = readResourceAsBytes("/example-dpp/dpp-unknown-ld.json");
        }
        Buffer buffer = Buffer.buffer(bytes);
        HttpResponse<Buffer> response = Mockito.mock(HttpResponse.class);
        Mockito.doReturn(buffer).when(response).bodyAsBuffer();
        if (type == ExtractionStrategyType.PLAIN_JSON) {
            Mockito.doReturn("application/json")
                    .when(response)
                    .getHeader(ArgumentMatchers.eq("Content-Type"));
        } else {
            Mockito.doReturn("application/ld+json")
                    .when(response)
                    .getHeader(ArgumentMatchers.eq("Content-Type"));
        }
        return response;
    }
}
