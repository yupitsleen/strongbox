package org.carlspring.strongbox.service.impl;

import org.carlspring.strongbox.config.ClientConfig;
import org.carlspring.strongbox.service.ProxyRepositoryConnectionPoolConfigurationService;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author korest
 */
@Disabled
@SpringBootTest
@ActiveProfiles(profiles = "test")
@ContextConfiguration
public class ProxyRepositoryConnectionPoolConfigurationServiceImplIntegrationTest
{

    @Configuration
    @Import({ClientConfig.class})
    public static class SpringConfig
    {
    }

    @Inject
    private ProxyRepositoryConnectionPoolConfigurationService proxyRepositoryConnectionPoolConfigurationService;

    @Test
    public void setMaxPoolSize()
    {
        proxyRepositoryConnectionPoolConfigurationService.setMaxTotal(10);
        assertEquals(10, proxyRepositoryConnectionPoolConfigurationService.getTotalStats().getMax());
    }

    @Test
    public void setDefaultMaxPerRepository()
    {
        proxyRepositoryConnectionPoolConfigurationService.setDefaultMaxPerRepository(8);
        assertEquals(8, proxyRepositoryConnectionPoolConfigurationService.getDefaultMaxPerRepository());
    }

    @Test
    public void setMaxPerRepository()
    {
        String repositoryUrl = "http://repo.spring.io/snapshot";
        proxyRepositoryConnectionPoolConfigurationService.setMaxPerRepository(repositoryUrl, 3);
        assertEquals(3, proxyRepositoryConnectionPoolConfigurationService.getPoolStats(repositoryUrl).getMax());
    }

    // integration test, external call to repo
    @Test
    public void connectionsReleasedTest()
    {
        String repositoryUrl = "http://repo.spring.io/snapshot";
        for (int i = 0; i < 3; i++)
        {
            Client client = proxyRepositoryConnectionPoolConfigurationService.getRestClient();
            Response response = client.target(repositoryUrl).request().get();
            response.close();
            client.close();
        }

        // all connections should be released
        assertEquals(0, proxyRepositoryConnectionPoolConfigurationService.getTotalStats().getLeased());
    }

    // integration test, external call to repo
    @Test
    public void connectionsLeakedTest()
    {
        String repositoryUrl = "http://repo.spring.io/snapshot";
        proxyRepositoryConnectionPoolConfigurationService.setMaxPerRepository(repositoryUrl, 3);
        for (int i = 0; i < 3; i++)
        {
            proxyRepositoryConnectionPoolConfigurationService.getRestClient().target(repositoryUrl).request().get();
        }

        // all connections should be leaked
        assertEquals(3, proxyRepositoryConnectionPoolConfigurationService.getTotalStats().getLeased());
    }
}
