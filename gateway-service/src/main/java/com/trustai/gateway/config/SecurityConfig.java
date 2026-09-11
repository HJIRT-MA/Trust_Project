package com.trustai.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final SseBearerTokenServerConverter sseBearerTokenServerConverter;

    public SecurityConfig(SseBearerTokenServerConverter sseBearerTokenServerConverter) {
        this.sseBearerTokenServerConverter = sseBearerTokenServerConverter;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                // Le handshake HTTP d'upgrade WebSocket doit rester public : l'authentification
                // du canal STOMP se fait via le frame CONNECT (header Authorization applicatif),
                // pas via un Bearer token sur la requête HTTP d'upgrade. rag-service applique la
                // même règle sur /ws/** (voir SecurityConfig côté rag-service).
                .pathMatchers("/ws/**").permitAll()
                // Sécuriser tout le reste
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .bearerTokenConverter(sseBearerTokenServerConverter)
                    .jwt(Customizer.withDefaults()))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(Customizer.withDefaults());

        return http.build();
    }
}
