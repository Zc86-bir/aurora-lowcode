package com.aurora.core.adapter.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Frontend Cache Strategy — Production-grade CDN & browser caching.
 *
 * Implements the Vite 6 asset fingerprinting caching strategy:
 * - /index.html → NO cache (must-revalidate, ensures instant updates on deploy)
 * - /assets/** → IMMUTABLE cache (max-age=31536000, assets have content hash)
 * - /static/** → LONG cache (max-age=86400, non-hashed static files)
 * - /favicon.ico → LONG cache (max-age=86400)
 *
 * When deployed behind CDN (Cloudflare, CloudFront, etc.), the immutable headers
 * allow edge caches to serve assets without revalidation.
 */
@Configuration
public class FrontendCacheConfig implements WebMvcConfigurer {

    private static final Duration ASSETS_MAX_AGE = Duration.ofDays(365);
    private static final Duration STATIC_MAX_AGE = Duration.ofDays(1);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Assets with hash fingerprint — immutable cache
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/static/assets/")
            .setCachePeriod((int) ASSETS_MAX_AGE.toSeconds())
            .setCacheControl(
                org.springframework.http.CacheControl.maxAge(ASSETS_MAX_AGE)
                    .cachePublic()
                    .mustRevalidate()
            );

        // Static files without hash — shorter cache
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod((int) STATIC_MAX_AGE.toSeconds())
            .setCacheControl(
                org.springframework.http.CacheControl.maxAge(STATIC_MAX_AGE)
                    .cachePublic()
            );

        // Favicon
        registry.addResourceHandler("/favicon.ico")
            .addResourceLocations("classpath:/static/favicon.ico")
            .setCachePeriod((int) STATIC_MAX_AGE.toSeconds())
            .setCacheControl(
                org.springframework.http.CacheControl.maxAge(STATIC_MAX_AGE)
                    .cachePublic()
            );
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // /index.html must never be cached — ensures instant updates on deploy
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler) {
                String path = request.getRequestURI();

                // HTML entry point — absolutely no caching
                if (path.equals("/") || path.equals("/index.html")) {
                    response.setHeader("Cache-Control",
                        "no-cache, no-store, must-revalidate, max-age=0");
                    response.setHeader("Pragma", "no-cache");
                    response.setHeader("Expires", "0");
                }

                // Service worker — no caching
                if (path.equals("/sw.js") || path.equals("/service-worker.js")) {
                    response.setHeader("Cache-Control",
                        "no-cache, no-store, must-revalidate, max-age=0");
                }

                return true;
            }
        });
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}".split(","))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600);
    }
}
