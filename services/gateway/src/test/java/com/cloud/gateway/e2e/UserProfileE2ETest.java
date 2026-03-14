package com.cloud.gateway.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
class UserProfileE2ETest {

    @BeforeAll
    static void init() {
        E2ETestSupport.initRestAssured();
    }

    @Test
    void shouldFetchCurrentUserProfile() {
        E2ETestSupport.assumeHasAccessToken();

        Response response = given()
                .header("Authorization", "Bearer " + E2ETestSupport.accessToken())
                .when()
                .get("/api/user/profile/current");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getInt("code")).isEqualTo(200);
        Object data = response.jsonPath().get("data");
        assertThat(data).isNotNull();
    }
}
