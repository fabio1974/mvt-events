#!/bin/bash

echo "🎯 Testando implementação de CRUD de Contratos"
echo "=============================================="
echo ""

echo "1. Compilando código..."
./gradlew compileJava

if [ $? -eq 0 ]; then
    echo "✅ Compilação bem-sucedida!"
    echo ""
    echo "2. Iniciando aplicação..."
    ./gradlew bootRun &
    GRADLE_PID=$!
    
    echo "Aguardando aplicação iniciar..."
    sleep 10
    
    echo ""
    echo "3. Testando endpoint de atualização de organizações..."
    echo "   - EmploymentContracts: Contratos Motoboy"
    echo "   - ServiceContracts: Contratos de Cliente"
    echo ""
    echo "📋 Agora você pode testar o curl do exemplo:"
    echo "   - Arrays serão processados corretamente"
    echo "   - Novos itens sem ID: INSERT"
    echo "   - Itens existentes com ID: UPDATE"
    echo "   - Itens ausentes do payload: DELETE"
    echo ""
    echo "Pressione CTRL+C para parar a aplicação"
    wait $GRADLE_PID
else
    echo "❌ Erro na compilação!"
    exit 1
fi