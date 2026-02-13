package it.extrared.extractor.api.rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.extrared.extractor.config.ExtractionConfiguration;
import it.extrared.extractor.utils.JsonUtils;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ExtractionConfigResourceTest {

    @Test
    public void testAddGetAddGetRemoveGet() throws IOException {
        ExtractionConfiguration configuration =
                JsonUtils.loadClasspathJsonAs(
                        "test-db-extraction-1.json", ExtractionConfiguration.class);
        given().when()
                .body(configuration)
                .contentType(ContentType.JSON)
                .post("/config/v1")
                .then()
                .statusCode(201);
        ExtractionConfiguration ret =
                given().when()
                        .get("/config/v1/current")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(ExtractionConfiguration.class);
        assertEquals(ret, configuration);
        ExtractionConfiguration config2 =
                JsonUtils.loadClasspathJsonAs(
                        "test-db-extraction-2.json", ExtractionConfiguration.class);
        given().when()
                .body(config2)
                .contentType(ContentType.JSON)
                .post("/config/v1")
                .then()
                .statusCode(201);
        ret =
                given().when()
                        .get("/config/v1/current")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(ExtractionConfiguration.class);
        assertEquals(config2, ret);
        given().when().delete("/config/v1/current").then().statusCode(204);
        ret =
                given().when()
                        .get("/config/v1/current")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(ExtractionConfiguration.class);
        assertEquals(configuration, ret);
        given().when().delete("/config/v1/current").then().statusCode(204);
    }
}
