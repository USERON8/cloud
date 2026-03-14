package com.cloud.gateway.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
class SearchFlowE2ETest {

    @BeforeAll
    static void init() {
        E2ETestSupport.initRestAssured();
    }

    @Test
    void shouldSearchAndSuggest() {
        E2ETestSupport.assumeHasAccessToken();

        String keyword = getKeyword();

        Response searchResponse = given()
                .header("Authorization", "Bearer " + E2ETestSupport.accessToken())
                .when()
                .get("/api/search/search?keyword=" + keyword + "&page=0&size=20");

        assertThat(searchResponse.statusCode()).isEqualTo(200);
        assertThat(searchResponse.jsonPath().getInt("code")).isEqualTo(200);

        Response suggestionResponse = given()
                .header("Authorization", "Bearer " + E2ETestSupport.accessToken())
                .when()
                .get("/api/search/suggestions?keyword=" + keyword + "&size=10");

        assertThat(suggestionResponse.statusCode()).isEqualTo(200);
        assertThat(suggestionResponse.jsonPath().getInt("code")).isEqualTo(200);
    }

    private String getKeyword() {
        String value = System.getenv("E2E_SEARCH_KEYWORD");
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty("E2E_SEARCH_KEYWORD", "phone");
        }
        return value;
    }
}
