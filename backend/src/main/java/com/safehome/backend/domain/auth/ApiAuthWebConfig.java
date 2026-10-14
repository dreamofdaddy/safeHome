package com.safehome.backend.domain.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class ApiAuthWebConfig implements WebMvcConfigurer {
    private final ApiAuthInterceptor interceptor;
    private final String[] allowedOrigins;

    public ApiAuthWebConfig(ApiAuthInterceptor interceptor,
            @Value("${safehome.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
            String allowedOrigins) {
        this.interceptor = interceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim).filter(value -> !value.isBlank()).toArray(String[]::new);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(interceptor).addPathPatterns("/api/**"); }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type")
            .exposedHeaders("Location")
            .maxAge(3600);
    }
}
