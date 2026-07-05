package com.intern.trustai.security;

import com.nimbusds.jwt.JWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();

            String tenantId = jwt.getClaimAsString("organisation_id");
            if(tenantId == null || tenantId.isEmpty()){
                String issuer = jwt.getIssuer().toString();
                tenantId = issuer.substring(issuer.lastIndexOf('/') + 1);
            }

            TenantContext.setCurrentTenant(tenantId);

        }else {
            TenantContext.setCurrentTenant("default_tenant");        }

        try{
            filterChain.doFilter(request,response);
        }finally {
            TenantContext.clear();
        }
    }


}
