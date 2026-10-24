package com.bankcore.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Argon2id is the password hashing algorithm required by TECHNICAL-DESIGN #14.
 * Spring Security's implementation needs BouncyCastle on the classpath.
 */
@Configuration
public class PasswordConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        // Argon2id with 19 MiB, 2 iterations and parallelism 1 (OWASP baseline).
        return new Argon2PasswordEncoder(16, 32, 1, 19_456, 2);
    }
}
