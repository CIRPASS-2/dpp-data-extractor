package it.extrared.extractor.api.rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import it.extrared.extractor.config.ExtractionConfigCache;
import it.extrared.extractor.config.field.FieldType;
import it.extrared.extractor.config.field.SearchField;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SearchCapabilitiesResourceTest {

    @Inject ExtractionConfigCache cache;

    @Test
    public void testGetCapabilities() {
        cache.invalidate();
        SearchField[] result =
                given().when()
                        .get("/capabilities/v1")
                        .then()
                        .extract()
                        .body()
                        .as(SearchField[].class);
        assertEquals(4, result.length);
        int hit = 0;
        for (SearchField f : result) {
            if (f.getFieldName().equals("productName")) {
                assertEquals(FieldType.STRING, f.getTargetType());
                hit += 1;
            }
            if (f.getFieldName().equals("weight")) {
                assertEquals(FieldType.DECIMAL, f.getTargetType());
                hit += 1;
            }
            if (f.getFieldName().equals("height")) {
                assertEquals(FieldType.DECIMAL, f.getTargetType());
                hit += 1;
            }
            if (f.getFieldName().equals("carbonFootprint")) {
                assertEquals(FieldType.DECIMAL, f.getTargetType());
                hit += 1;
            }
        }
        assertEquals(4, hit);
    }
}
