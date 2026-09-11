package com.trustai.gateway.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Mirrors rag-service's SseBearerTokenResolver (Servlet) for the reactive gateway.
 * The browser's native EventSource API cannot set an Authorization header, so the
 * audit SSE stream (/api/audit/stream/{contractId}) is called with a `token` query
 * parameter instead. This converter only falls back to that query parameter for that
 * one path, so no other route gains the weaker, referrer-leakable query-param auth path.
 */
@Component
public class SseBearerTokenServerConverter implements ServerAuthenticationConverter {

    private static final String SSE_PATH_MARKER = "/api/audit/stream/";

    private final ServerBearerTokenAuthenticationConverter headerConverter = new ServerBearerTokenAuthenticationConverter();

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return headerConverter.convert(exchange)
                .switchIfEmpty(Mono.defer(() -> {
                    String path = exchange.getRequest().getURI().getPath();
                    if (path.contains(SSE_PATH_MARKER)) {
                        String token = exchange.getRequest().getQueryParams().getFirst("token");
                        if (token != null && !token.isEmpty()) {
                            return Mono.just(new BearerTokenAuthenticationToken(token));
                        }
                    }
                    return Mono.empty();
                }));
    }
}
