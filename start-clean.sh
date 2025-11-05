#!/bin/zsh

echo "🔄 Parando processos Gradle..."
pkill -f gradle
sleep 2

cd /Users/jose.barros.br/Documents/projects/mvt-events

echo "🧹 Limpando build..."
./gradlew clean

echo ""
echo "🔨 Compilando..."
./gradlew compileJava 2>&1 | tee compile-result.txt

echo ""
if grep -q "BUILD SUCCESSFUL" compile-result.txt; then
    echo "✅ COMPILAÇÃO OK!"
    echo ""
    echo "🚀 Iniciando aplicação..."
    ./gradlew bootRun
else
    echo "❌ ERRO NA COMPILAÇÃO"
    echo ""
    echo "Erros encontrados:"
    grep "error:" compile-result.txt | head -10
fi
