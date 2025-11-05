#!/bin/bash

echo "===== DIAGNÓSTICO RÁPIDO ====="
cd /Users/jose.barros.br/Documents/projects/mvt-events

echo -e "\n1. Verificando linha 36-38 do UnifiedPayoutService:"
sed -n '35,40p' src/main/java/com/mvt/mvt_events/service/UnifiedPayoutService.java

echo -e "\n2. Verificando se PaymentRepository existe:"
ls -la src/main/java/com/mvt/mvt_events/repository/PaymentRepository.java

echo -e "\n3. Limpando build e compilando:"
./gradlew clean compileJava --no-daemon 2>&1 | tail -20

echo -e "\n===== FIM ====="
