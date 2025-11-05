#!/bin/bash

echo "========================================"
echo "Limpeza Final do Código..."
echo "========================================"

echo ""
echo "1. Removendo payment providers do source..."
# Remove o arquivo StripePaymentProvider.java do source
rm -f "src/main/java/com/mvt/mvt_events/payment/providers/StripePaymentProvider.java"
echo "   ✅ StripePaymentProvider.java removido"

# Remove o diretório providers (se vazio)
rmdir "src/main/java/com/mvt/mvt_events/payment/providers" 2>/dev/null
echo "   ✅ Diretório providers removido"

echo ""
echo "2. Removendo entidade CourierOrganization obsoleta..."
# Remove entidade CourierOrganization (substituída por EmploymentContract)
rm -f "src/main/java/com/mvt/mvt_events/jpa/CourierOrganization.java"
echo "   ✅ CourierOrganization.java removido"

echo ""
echo "========================================"
echo "Limpando build..."
echo "========================================"
./gradlew clean

echo ""
echo "========================================"
echo "Compilando..."
echo "========================================"
./gradlew compileJava

echo ""
echo "✅ Cleanup completo!"
echo ""
echo "Agora execute: ./start-app.sh"
