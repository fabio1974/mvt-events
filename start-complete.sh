#!/bin/bash

echo "========================================"
echo "🚀 Inicialização Completa do Sistema"
echo "========================================"

echo ""
echo "📦 Limpando build anterior..."
./gradlew clean

echo ""
echo "🔨 Compilando código..."
./gradlew compileJava

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Compilação bem-sucedida!"
    echo ""
    echo "🔄 Iniciando aplicação..."
    ./gradlew bootRun
else
    echo ""
    echo "❌ Erro na compilação!"
    echo "Verifique os erros acima."
    exit 1
fi
