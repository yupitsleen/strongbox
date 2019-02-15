package org.carlspring.strongbox.controllers;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.rest.common.MavenRestAssuredBaseTest;

import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * @author Steve Todorov
 */
@IntegrationTest
public class PingControllerTest
        extends MavenRestAssuredBaseTest
{

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();
        setContextBaseUrl(getContextBaseUrl() + "/api/ping");
    }

    @Test
    public void testShouldReturnPongText()
    {

        given().header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE)
               .when()
               .get(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(equalTo("pong"));
    }

    @Test
    public void testShouldReturnPongJSON()
    {
        given().header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body("message", equalTo("pong"));
    }

    @Test
    public void testShouldReturnPongForAuthenticatedUsersJSON()
    {
        given().header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl() + "/token")
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body("message", equalTo("pong"));
    }

    @Test
    public void testShouldReturnPongForAuthenticatedUsersText()
    {
        given().header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE)
               .when()
               .get(getContextBaseUrl() + "/token")
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(equalTo("pong"));
    }

    @Test
    @WithAnonymousUser
    public void testAnonymousUsersShouldNotBeAbleToAccessJSON()
    {
        given().header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl() + "/token")
               .peek()
               .then()
               .log().all()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body(notNullValue());
    }

    @Test
    @WithAnonymousUser
    public void testAnonymousUsersShouldNotBeAbleToAccessText()
    {
        given().header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE)
               .when()
               .get(getContextBaseUrl() + "/token")
               .peek()
               .then()
               .log().all()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body(notNullValue());
    }
}
