#!/bin/bash

echo "🔧 Testando correções de compilação..."
echo ""

echo "1. Compilando..."
./gradlew compileJava

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Compilação corrigida com sucesso!"
    echo ""
    echo "📋 Problemas resolvidos:"
    echo "   ✅ Getters/setters adicionados com @Data e @NoArgsConstructor"
    echo "   ✅ Métodos duplicados nos repositories renomeados"
    echo "   ✅ Chamadas atualizadas no service"
    echo ""
    echo "🚀 Agora pode testar o curl dos contratos!"
    echo ""
    echo "Execute: ./start-app.sh"
else
    echo ""
    echo "❌ Ainda há erros de compilação"
    echo "Verificar logs acima..."
fi