#!/bin/bash

echo "========================================"
echo "Teste de Boot - Zapi10"
echo "========================================"
echo ""

cd /Users/jose.barros.br/Documents/projects/mvt-events

# Matar processos gradle anteriores
echo "🔄 Parando processos gradle anteriores..."
pkill -f gradle
sleep 2

# Limpar build
echo "🧹 Limpando build..."
rm -rf build/
./gradlew clean > /dev/null 2>&1

# Compilar
echo "🔨 Compilando..."
./gradlew compileJava 2>&1 | tee compile.log

if grep -q "BUILD SUCCESSFUL" compile.log; then
    echo "✅ Compilação: OK"
    
    # Tentar subir aplicação
    echo ""
    echo "🚀 Iniciando aplicação..."
    echo "📝 Logs em: bootrun-test.log"
    echo ""
    echo "Aguardando 15 segundos..."
    
    # Subir em background
    ./gradlew bootRun > bootrun-test.log 2>&1 &
    BOOT_PID=$!
    echo "PID: $BOOT_PID"
    
    # Aguardar
    sleep 15
    
    # Verificar se subiu
    if grep -q "Started MvtEventsApplication" bootrun-test.log; then
        echo ""
        echo "✅ ✅ ✅ APLICAÇÃO SUBIU COM SUCESSO! ✅ ✅ ✅"
        echo ""
        echo "🌐 Acesse: http://localhost:8080"
        echo ""
        echo "Para ver os logs:"
        echo "  tail -f bootrun-test.log"
        echo ""
        echo "Para parar:"
        echo "  kill $BOOT_PID"
        echo ""
    else
        echo ""
        echo "❌ Erro ao subir aplicação"
        echo ""
        echo "📋 Últimas 50 linhas do log:"
        tail -50 bootrun-test.log
        
        # Matar processo
        kill $BOOT_PID 2>/dev/null
    fi
    
else
    echo "❌ Erro na compilação"
    echo ""
    echo "📋 Erros encontrados:"
    grep "error:" compile.log | head -20
fi

