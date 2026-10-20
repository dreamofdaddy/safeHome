package com.safehome.integration;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalAccessFilter extends OncePerRequestFilter {
    private final byte[] expected;
    public InternalAccessFilter(@Value("${safehome.integration.token}") String token) {
        expected = token.getBytes(StandardCharsets.UTF_8);
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                             FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().equals("/actuator/health")) {
            chain.doFilter(request, response);
            return;
        }
        String token = request.getHeader("X-SafeHome-Integration-Token");
        if (expected.length < 32) { response.setStatus(503); return; }
        if (token == null || !MessageDigest.isEqual(expected, token.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(401); return;
        }
        chain.doFilter(request, response);
    }
}
