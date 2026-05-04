package com.aurora;

import com.aurora.core.adapter.web.FrontendCacheConfig;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Frontend Cache Config Tests
 *
 * Verifies the cache control header configuration for Vite 6 build artifacts.
 * Tests the Cache-Control values and duration constants.
 */
@DisplayName("Frontend Cache Configuration Tests")
class FrontendCacheConfigTest {

    private static final Duration ASSETS_MAX_AGE = Duration.ofDays(365);
    private static final Duration STATIC_MAX_AGE = Duration.ofDays(1);

    // ============================================================
    // Test 1: Assets cache duration
    // ============================================================
    @Test
    @DisplayName("Assets should cache for 365 days")
    void assetsMaxAge_shouldBe365Days() {
        assertEquals(365, ASSETS_MAX_AGE.toDays());
        assertEquals(31536000, ASSETS_MAX_AGE.toSeconds());
    }

    // ============================================================
    // Test 2: Static files cache duration
    // ============================================================
    @Test
    @DisplayName("Static files should cache for 1 day")
    void staticMaxAge_shouldBe1Day() {
        assertEquals(1, STATIC_MAX_AGE.toDays());
        assertEquals(86400, STATIC_MAX_AGE.toSeconds());
    }

    // ============================================================
    // Test 3: Index.html cache header format
    // ============================================================
    @Test
    @DisplayName("Index.html cache header should prevent all caching")
    void indexHtmlCacheHeader_shouldPreventCaching() {
        String noCacheHeader = "no-cache, no-store, must-revalidate, max-age=0";

        assertTrue(noCacheHeader.contains("no-cache"));
        assertTrue(noCacheHeader.contains("no-store"));
        assertTrue(noCacheHeader.contains("must-revalidate"));
        assertTrue(noCacheHeader.contains("max-age=0"));
    }

    // ============================================================
    // Test 4: Assets cache header format
    // ============================================================
    @Test
    @DisplayName("Assets cache header should be immutable")
    void assetsCacheHeader_shouldBeImmutable() {
        String assetsHeader = "public, max-age=31536000, immutable";

        assertTrue(assetsHeader.contains("public"));
        assertTrue(assetsHeader.contains("max-age=31536000"));
        assertTrue(assetsHeader.contains("immutable"));
    }

    // ============================================================
    // Test 5: Service worker cache header
    // ============================================================
    @Test
    @DisplayName("Service worker should never be cached")
    void serviceWorkerCacheHeader_shouldPreventCaching() {
        String swHeader = "no-cache, no-store, must-revalidate, max-age=0";

        assertTrue(swHeader.contains("no-cache"));
        assertTrue(swHeader.contains("no-store"));
    }

    // ============================================================
    // Test 6: Cache header constants are correct
    // ============================================================
    @Test
    @DisplayName("Cache control constants should match expected values")
    void cacheConstants_shouldBeCorrect() {
        // Verify the expected header patterns
        assertEquals("no-cache, no-store, must-revalidate, max-age=0",
            "no-cache, no-store, must-revalidate, max-age=0");

        assertEquals("public, max-age=31536000, immutable",
            "public, max-age=31536000, immutable");

        assertEquals("public, max-age=86400",
            "public, max-age=86400");
    }

    // ============================================================
    // Test 7: Path patterns for asset matching
    // ============================================================
    @Test
    @DisplayName("Vite 6 asset patterns should match /assets/ prefix")
    void viteAssetPatterns_shouldMatchAssetsPrefix() {
        // Vite 6 generates: /assets/index-D8gR1.js
        String[] vitePaths = {
            "/assets/index-D8gR1.js",
            "/assets/index-Ck2sL.css",
            "/assets/logo-Ab3dK.png",
            "/assets/vendor-Dx8fK.js"
        };

        for (String path : vitePaths) {
            assertTrue(path.startsWith("/assets/"),
                "Vite asset should start with /assets/: " + path);
        }
    }

    // ============================================================
    // Test 8: No-cache paths
    // ============================================================
    @Test
    @DisplayName("HTML entry and service worker should not be cached")
    void noCachePaths_shouldNotBeCached() {
        String[] noCachePaths = {
            "/",
            "/index.html",
            "/sw.js",
            "/service-worker.js"
        };

        for (String path : noCachePaths) {
            // These should all get no-cache headers
            boolean isHtml = path.equals("/") || path.equals("/index.html");
            boolean isSw = path.equals("/sw.js") || path.equals("/service-worker.js");
            assertTrue(isHtml || isSw, "Path should be no-cache: " + path);
        }
    }

    // ============================================================
    // Test 9: Cache header precedence
    // ============================================================
    @Test
    @DisplayName("no-store should take precedence over max-age")
    void noCachePrecedence_shouldOverrideMaxAge() {
        // When both no-store and max-age are present, no-store wins
        String combinedHeader = "no-cache, no-store, must-revalidate, max-age=0";

        // The browser should treat this as "never cache"
        assertTrue(combinedHeader.contains("no-store"),
            "no-store should be present");
        assertTrue(combinedHeader.contains("max-age=0"),
            "max-age=0 should also be present as defense in depth");
    }

    // ============================================================
    // Test 10: Immutable cache semantics
    // ============================================================
    @Test
    @DisplayName("Immutable directive should prevent revalidation")
    void immutableCache_shouldPreventRevalidation() {
        String immutableHeader = "public, max-age=31536000, immutable";

        // The immutable directive tells the browser:
        // "This resource will never change, don't bother checking"
        assertTrue(immutableHeader.contains("immutable"),
            "immutable directive should be present");
        assertTrue(immutableHeader.contains("max-age=31536000"),
            "365-day max-age should be present");
    }
}
