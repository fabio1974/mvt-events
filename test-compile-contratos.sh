#!/bin/bash

echo "🔧 Compilando implementação de CRUD de Contratos..."
./gradlew compileJava

if [ $? -eq 0 ]; then
    echo "✅ Compilação bem-sucedida!"
    echo ""
    echo "🚀 Agora você pode testar o curl com os contratos!"
    echo ""
    echo "📋 O que foi implementado:"
    echo "   - employmentContracts serão salvos como Contratos Motoboy"
    echo "   - serviceContracts serão salvos como Contratos de Cliente"
    echo "   - Arrays fazem DELETE completo + INSERT dos novos"
    echo "   - Traduções em português implementadas"
    echo ""
    echo "Execute ./start-app.sh para iniciar a aplicação"
else
    echo "❌ Erro na compilação - verificando logs..."
fi