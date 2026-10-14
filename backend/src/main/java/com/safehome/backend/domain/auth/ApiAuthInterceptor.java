package com.safehome.backend.domain.auth;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiAuthInterceptor implements HandlerInterceptor {
    private final AuthService auth;
    private final boolean enforce;
    private final boolean legacyAdminApiEnabled;
    private static final String[] LEGACY_ADMIN_PATHS = {
        "/api/contracts", "/api/contract-special-terms", "/api/properties",
        "/api/property-owners", "/api/registry-snapshots", "/api/registry-rights",
        "/api/risk-analyses", "/api/risk-factors"
    };

    public ApiAuthInterceptor(AuthService auth,
            @Value("${safehome.auth.enforce:true}") boolean enforce,
            @Value("${safehome.legacy-admin-api.enabled:false}") boolean legacyAdminApiEnabled) {
        this.auth = auth;
        this.enforce = enforce;
        this.legacyAdminApiEnabled = legacyAdminApiEnabled;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if ("OPTIONS".equals(request.getMethod())
                || path.equals("/api/auth/signup") || path.equals("/api/auth/login")) return true;
        if (enforce) auth.requireUser(request.getHeader("Authorization"));
        if (!legacyAdminApiEnabled && isLegacyAdminPath(path)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return true;
    }

    private boolean isLegacyAdminPath(String path) {
        for (String prefix : LEGACY_ADMIN_PATHS) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) return true;
        }
        return false;
    }
}
