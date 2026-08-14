package com.intern.trustai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;

@Configuration
public class SecuritySignatureConfig {

    @Bean
    public PrivateKey reportSignaturePrivateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new ClassPathResource("keystore.p12").getInputStream()) {
            keyStore.load(is, "trustaipass".toCharArray());
        }
        return (PrivateKey) keyStore.getKey("trustai-key", "trustaipass".toCharArray());
    }
}
