#!/usr/bin/env bash
# scripts/start-caddy.sh — Start Caddy for Aurora local dev
# Usage: ./scripts/start-caddy.sh [start|stop|reload]
#
# Prerequisites:
#   1. Download caddy from https://github.com/caddyserver/caddy/releases
#   2. Place caddy binary in /e/caddy/
#   3. ./scripts/start-caddy.sh start   # start caddy reverse proxy

CADDY_PATH="/e/caddy"
CONF_SRC="deploy/caddy-local.Caddyfile"
FRONTEND_BUILD="frontend/dist"
CADDY_HTML="${CADDY_PATH}/html/dist"

case "${1:-start}" in
    start)
        echo "Copying frontend build..."
        if [ -d "$FRONTEND_BUILD" ]; then
            cp -r "$FRONTEND_BUILD"/* "$CADDY_HTML"/
        else
            echo "WARNING: frontend build not found at $FRONTEND_BUILD"
            echo "Run: cd frontend && pnpm build"
        fi
        echo "Starting caddy..."
        cd "$CADDY_PATH" && ./caddy run --config "$(pwd)/../../$CONF_SRC" &
        echo "Caddy running at http://localhost:8088"
        echo "  Frontend : http://localhost:8088"
        echo "  Backend  : http://localhost:8088/api/"
        echo "  Swagger  : http://localhost:8088/swagger-ui/"
        ;;
    stop)
        echo "Stopping caddy..."
        cd "$CADDY_PATH" && ./caddy stop
        echo "Caddy stopped."
        ;;
    reload)
        echo "Reloading caddy config..."
        cd "$CADDY_PATH" && ./caddy reload --config "$(pwd)/../../$CONF_SRC"
        echo "Caddy config reloaded."
        ;;
    test)
        cd "$CADDY_PATH" && ./caddy validate --config "$(pwd)/../../$CONF_SRC"
        ;;
    *)
        echo "Usage: $0 [start|stop|reload|test]"
        ;;
esac
