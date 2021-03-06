package org.carlspring.strongbox.configuration;

import javax.enterprise.inject.Default;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ComponentScan({
        "org.carlspring.strongbox.configuration",
        "org.carlspring.strongbox.security",
        "org.carlspring.strongbox.visitors",
})
public class StrongboxSecurityConfig
{

    private static final Logger logger = LoggerFactory.getLogger(StrongboxSecurityConfig.class);
    
    @Bean
    @Default
    PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

}
