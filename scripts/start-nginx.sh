#!/bin/bash
# scripts/start-nginx.sh — Start nginx for Aurora local dev
# Usage: ./scripts/start-nginx.sh [start|stop|reload]
# Prerequisites:
#   1. cd frontend && pnpm build        # build frontend to dist/
#   2. mvn spring-boot:run              # start backend
#   3. ./scripts/start-nginx.sh start   # start nginx reverse proxy

NGINX_PATH="/e/nginx-1.29.8"
CONF_SRC="deploy/nginx-local.conf"
CONF_DST="${NGINX_PATH}/conf/nginx.conf"

case "${1:-start}" in
  start)
    echo "Copying nginx config..."
    cp "$CONF_SRC" "$CONF_DST"
    echo "Starting nginx (PID file: ${NGINX_PATH}/logs/nginx.pid)..."
    cd "$NGINX_PATH" && ./nginx.exe
    echo "Nginx running at http://localhost:80"
    echo "  Frontend (built): http://localhost:80"
    echo "  Backend API:      http://localhost:80/api/"
    echo "  Swagger UI:       http://localhost:80/swagger-ui/"
    ;;
  stop)
    echo "Stopping nginx..."
    cd "$NGINX_PATH" && ./nginx.exe -s quit
    echo "Nginx stopped."
    ;;
  reload)
    cp "$CONF_SRC" "$CONF_DST"
    cd "$NGINX_PATH" && ./nginx.exe -s reload
    echo "Nginx reloaded."
    ;;
  test)
    cd "$NGINX_PATH" && ./nginx.exe -t
    ;;
  *)
    echo "Usage: $0 [start|stop|reload|test]"
    ;;
esac
