#!/usr/bin/env sh
set -e
docker compose up --build -d --wait
open http://localhost:8081 2>/dev/null || xdg-open http://localhost:8081
docker compose logs -f
