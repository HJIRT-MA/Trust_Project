package com.intern.trustai.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * The browser's native EventSource API cannot set an Authorization header, so the
 * audit SSE stream cannot be authenticated the normal way. This resolver falls back
 * to a `token` query parameter, but only for that one endpoint, so no other route
 * gains the (weaker, referrer-leakable) query-param auth path.
 */
@Component
public class SseBearerTokenResolver implements BearerTokenResolver {

    private static final String SSE_PATH_MARKER = "/api/audit/stream/";

    private final DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        String token = defaultResolver.resolve(request);
        if (token != null) {
            return token;
        }
        if (request.getRequestURI().contains(SSE_PATH_MARKER)) {
            return request.getParameter("token");
        }
        return null;
    }
}
