# CDN Configuration Guide for Aurora Frontend
#
# This document describes how to configure popular CDNs
# for optimal caching of Vite 6 build artifacts.
#
# Vite 6 generates assets with content hash fingerprints:
# - /assets/index-D8gR1.js
# - /assets/index-Ck2sL.css
# - /assets/logo-Ab3dK.png
#
# Cache Strategy:
# ┌─────────────────────┬─────────────────────────────────────────┬──────────┐
# │ Path Pattern        │ Cache-Control Header                    │ Duration │
# ├─────────────────────┼─────────────────────────────────────────┼──────────┤
# │ /                   │ no-cache, no-store, must-revalidate     │ 0s       │
# │ /index.html         │ no-cache, no-store, must-revalidate     │ 0s       │
# │ /sw.js              │ no-cache, no-store, must-revalidate     │ 0s       │
# │ /assets/**          │ public, max-age=31536000, immutable     │ 365d     │
# │ /static/**          │ public, max-age=86400                   │ 1d       │
# │ /favicon.ico        │ public, max-age=86400                   │ 1d       │
# │ /*.woff2            │ public, max-age=2592000                 │ 30d      │
# │ /*.png,*.jpg,*.svg  │ public, max-age=604800                  │ 7d       │
# └─────────────────────┴─────────────────────────────────────────┴──────────┘

# ===========================
# Cloudflare (Page Rules)
# ===========================
# Via Cloudflare Dashboard → Rules → Page Rules:
#
# Rule 1: /index.html
#   URL Pattern: aurora.example.com/index.html
#   Settings:
#     - Cache Level: Bypass
#     - Edge Cache TTL: 0 seconds
#
# Rule 2: /assets/*
#   URL Pattern: aurora.example.com/assets/*
#   Settings:
#     - Cache Level: Cache Everything
#     - Edge Cache TTL: 1 year
#     - Browser Cache TTL: 1 year
#     - Respect Strong ETags: On
#
# Rule 3: /static/*
#   URL Pattern: aurora.example.com/static/*
#   Settings:
#     - Cache Level: Cache Everything
#     - Edge Cache TTL: 1 day

# ===========================
# AWS CloudFront (Behavior)
# ===========================
# Via CloudFront Distribution → Behaviors:
#
# Behavior 1: /index.html
#   Path Pattern: /index.html
#   Cache Policy: CachingDisabled
#   Origin Request Policy: AllViewer
#
# Behavior 2: /assets/*
#   Path Pattern: /assets/*
#   Cache Policy: CachingOptimized
#   Origin Request Policy: CORS-CustomOrigin
#   TTL Settings:
#     - Minimum: 0
#     - Maximum: 31536000
#     - Default: 31536000
#
# Behavior 3: /static/*
#   Path Pattern: /static/*
#   Cache Policy: CachingOptimized
#   TTL Settings:
#     - Default: 86400

# ===========================
# Google Cloud CDN
# ===========================
# Via GCP → Load Balancing → Backend Service → CDN:
#
# Enable CDN on the backend service, then set cache modes:
#
# Cache Mode: CACHE_ALL_STATIC
# Default TTL: 1 day
#
# Add CacheKey policies:
#   - Include Protocol: false
#   - Include Host: true
#   - Include Query String: false
#
# For /index.html, set a separate cache mode:
# Cache Mode: USE_ORIGIN_HEADERS
# (respects the origin's no-cache headers)

# ===========================
# Akamai
# ===========================
# Via Akamai Property Manager:
#
# Behavior 1: HTML Entry
#   Match Criteria: Path ends with .html
#   Cache-Control: no-cache, no-store, must-revalidate
#   TTL: 0s
#
# Behavior 2: Assets
#   Match Criteria: Path starts with /assets/
#   Cache-Control: public, max-age=31536000, immutable
#   TTL: 365d

# ===========================
# Fastly
# ===========================
# Via Fastly VCL or Compute@Edge:
#
# sub vcl_recv {
#   # HTML — never cache
#   if (req.url ~ "^/index\.html$" || req.url == "/") {
#     return(pass);
#   }
#
#   # Assets — cache forever
#   if (req.url ~ "^/assets/") {
#     set beresp.ttl = 365d;
#     set beresp.http.Cache-Control = "public, max-age=31536000, immutable";
#   }
# }
