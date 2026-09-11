package com.trustai.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SseBearerTokenServerConverterTest {

    private final SseBearerTokenServerConverter converter = new SseBearerTokenServerConverter();

    @Test
    void resolvesTokenFromQueryParamOnTheSseStreamPath() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/audit/stream/42")
                .queryParam("token", "abc123")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Authentication auth = converter.convert(exchange).block();

        BearerTokenAuthenticationToken token = assertInstanceOf(BearerTokenAuthenticationToken.class, auth);
        assertEquals("abc123", token.getToken());
    }

    @Test
    void doesNotFallBackToQueryParamOnAnyOtherPath() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/rag/documents")
                .queryParam("token", "abc123")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(converter.convert(exchange)).verifyComplete();
    }

    @Test
    void emitsNothingOnTheSseStreamPathWhenNoTokenIsPresentAtAll() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/audit/stream/42")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(converter.convert(exchange)).verifyComplete();
    }

    @Test
    void authorizationHeaderTakesPrecedenceOverTheQueryParam() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/audit/stream/42")
                .header("Authorization", "Bearer header-token")
                .queryParam("token", "query-token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Authentication auth = converter.convert(exchange).block();

        BearerTokenAuthenticationToken token = assertInstanceOf(BearerTokenAuthenticationToken.class, auth);
        assertEquals("header-token", token.getToken());
    }
}
