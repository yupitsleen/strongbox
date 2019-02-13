package org.carlspring.strongbox.security.managers;

import org.carlspring.strongbox.config.CommonConfig;
import org.carlspring.strongbox.configuration.StrongboxSecurityConfig;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author mtodorov
 */
@SpringBootTest
@ActiveProfiles(profiles = "test")
@ContextConfiguration
public class AuthenticationManagerTest
{

    @Configuration
    @Import({ CommonConfig.class,
              StrongboxSecurityConfig.class
              })

    public static class SpringConfig { }

    @Inject
    private AuthenticationManager authenticationManager;

    @Test
    public void testLoad()
    {
        assertNotNull(authenticationManager.getConfiguration(), "Failed to load configuration!");
        assertNotNull(authenticationManager.getRealms(), "Failed to load realms!");
        assertFalse(authenticationManager.getRealms().isEmpty(), "Failed to load realms!");
        assertNotNull(authenticationManager.getAnonymousAccessConfiguration(),
                      "Failed to load settings for anonymous access!");
        assertTrue(authenticationManager.getAnonymousAccessConfiguration().isEnabled(),
                   "Failed to load settings for anonymous access!");
    }

}
