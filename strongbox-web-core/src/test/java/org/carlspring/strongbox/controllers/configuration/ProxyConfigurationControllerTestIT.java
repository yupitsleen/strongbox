package org.carlspring.strongbox.controllers.configuration;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.configuration.MutableProxyConfiguration;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.carlspring.strongbox.controllers.configuration.ProxyConfigurationController.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Pablo Tirado
 */
@IntegrationTest
@SpringBootTest
public class ProxyConfigurationControllerTestIT
        extends RestAssuredBaseTest
{

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();
        setContextBaseUrl("/api/configuration/strongbox/proxy-configuration");
    }

    static MutableProxyConfiguration createProxyConfiguration()
    {
        MutableProxyConfiguration proxyConfiguration = new MutableProxyConfiguration();
        proxyConfiguration.setHost("localhost");
        proxyConfiguration.setPort(8080);
        proxyConfiguration.setUsername("user1");
        proxyConfiguration.setPassword("pass2");
        proxyConfiguration.setType("http");
        List<String> nonProxyHosts = Lists.newArrayList();
        nonProxyHosts.add("localhost");
        nonProxyHosts.add("some-hosts.com");
        proxyConfiguration.setNonProxyHosts(nonProxyHosts);

        return proxyConfiguration;
    }

    private static MutableProxyConfiguration createWrongProxyConfiguration()
    {
        MutableProxyConfiguration proxyConfiguration = new MutableProxyConfiguration();
        proxyConfiguration.setHost("");
        proxyConfiguration.setPort(0);
        proxyConfiguration.setUsername("user1");
        proxyConfiguration.setPassword("pass2");
        proxyConfiguration.setType("TEST");
        List<String> nonProxyHosts = Lists.newArrayList();
        nonProxyHosts.add("localhost");
        nonProxyHosts.add("some-hosts.com");
        proxyConfiguration.setNonProxyHosts(nonProxyHosts);

        return proxyConfiguration;
    }

    private void testSetAndGetGlobalProxyConfiguration(String acceptHeader)
    {
        MutableProxyConfiguration proxyConfiguration = createProxyConfiguration();

        String url = getContextBaseUrl();

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(acceptHeader)
               .body(proxyConfiguration)
               .when()
               .put(url)
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(containsString(SUCCESSFUL_UPDATE));

        logger.debug("Current proxy host: " + proxyConfiguration.getHost());

        MutableProxyConfiguration pc = given().contentType(MediaType.APPLICATION_JSON_VALUE)
                                              .accept(MediaType.APPLICATION_JSON_VALUE)
                                              .when()
                                              .get(url)
                                              .as(MutableProxyConfiguration.class);

        assertNotNull(pc, "Failed to get proxy configuration!");
        assertEquals(proxyConfiguration.getHost(), pc.getHost(), "Failed to get proxy configuration!");
        assertEquals(proxyConfiguration.getPort(), pc.getPort(), "Failed to get proxy configuration!");
        assertEquals(proxyConfiguration.getUsername(), pc.getUsername(), "Failed to get proxy configuration!");
        assertEquals(proxyConfiguration.getPassword(), pc.getPassword(), "Failed to get proxy configuration!");
        assertEquals(proxyConfiguration.getType(), pc.getType(), "Failed to get proxy configuration!");
        assertEquals(proxyConfiguration.getNonProxyHosts(), pc.getNonProxyHosts(),
                     "Failed to get proxy configuration!");
    }

    @WithMockUser(authorities = {"CONFIGURATION_SET_GLOBAL_PROXY_CFG", "CONFIGURATION_VIEW_GLOBAL_PROXY_CFG"})
    @Test
    public void testSetAndGetGlobalProxyConfigurationWithTextAcceptHeader()
    {
        testSetAndGetGlobalProxyConfiguration(MediaType.TEXT_PLAIN_VALUE);
    }

    @WithMockUser(authorities = {"CONFIGURATION_SET_GLOBAL_PROXY_CFG", "CONFIGURATION_VIEW_GLOBAL_PROXY_CFG"})
    @Test
    public void testSetAndGetGlobalProxyConfigurationWithJsonAcceptHeader()
    {
        testSetAndGetGlobalProxyConfiguration(MediaType.APPLICATION_JSON_VALUE);
    }

    private void testSetGlobalProxyConfigurationNotFound(String acceptHeader)
    {
        MutableProxyConfiguration proxyConfiguration = createProxyConfiguration();

        String url = getContextBaseUrl();
        String storageId = "storage-not-found";
        String repositoryId = "repo-not-found";

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(acceptHeader)
               .body(proxyConfiguration)
               .param("storageId", storageId)
               .param("repositoryId", repositoryId)
               .when()
               .put(url)
               .then()
               .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
               .body(containsString(FAILED_UPDATE));

    }

    @WithMockUser(authorities = "CONFIGURATION_SET_GLOBAL_PROXY_CFG")
    @Test
    public void testSetGlobalProxyConfigurationNotFoundWithTextAcceptHeader()
    {
        testSetGlobalProxyConfigurationNotFound(MediaType.TEXT_PLAIN_VALUE);
    }

    @WithMockUser(authorities = "CONFIGURATION_SET_GLOBAL_PROXY_CFG")
    @Test
    public void testSetGlobalProxyConfigurationNotFoundWithJsonAcceptHeader()
    {
        testSetGlobalProxyConfigurationNotFound(MediaType.APPLICATION_JSON_VALUE);
    }

    private void testSetGlobalProxyConfigurationBadRequest(String acceptHeader)
    {
        MutableProxyConfiguration proxyConfiguration = createWrongProxyConfiguration();

        String url = getContextBaseUrl();

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(acceptHeader)
               .body(proxyConfiguration)
               .when()
               .put(url)
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .body(containsString(FAILED_UPDATE_FORM_ERROR));

    }

    @WithMockUser(authorities = "CONFIGURATION_SET_GLOBAL_PROXY_CFG")
    @Test
    public void testSetGlobalProxyConfigurationBadRequestWithTextAcceptHeader()
    {
        testSetGlobalProxyConfigurationBadRequest(MediaType.TEXT_PLAIN_VALUE);
    }

    @WithMockUser(authorities = "CONFIGURATION_SET_GLOBAL_PROXY_CFG")
    @Test
    public void testSetGlobalProxyConfigurationBadRequestWithJsonAcceptHeader()
    {
        testSetGlobalProxyConfigurationBadRequest(MediaType.APPLICATION_JSON_VALUE);
    }
}
