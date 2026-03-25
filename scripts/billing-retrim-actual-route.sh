#!/usr/bin/env bash
# Corta actual_route no Postgres (mesmo algoritmo de DeliveryService.complete).
# Carrega .env.local se existir (SPRING_DATASOURCE_* etc.).
set -euo pipefail
cd "$(dirname "$0")/.."
if [ "$#" -eq 0 ]; then
  echo "Uso: $0 <deliveryId> [<deliveryId> ...]" >&2
  echo "Ex.: $0 80" >&2
  exit 1
fi
IDS=$(printf '%s,' "$@" | sed 's/,$//')
if [ -f .env.local ]; then
  set -a
  # shellcheck source=/dev/null
  source .env.local
  set +a
fi
export SPRING_PROFILES_ACTIVE=billing-retrim
export APP_BILLING_RETRIM_IDS="$IDS"
exec ./gradlew bootRun --no-daemon
