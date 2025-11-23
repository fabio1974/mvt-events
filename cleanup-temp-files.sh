#!/bin/bash
# Script de limpeza do repositório - Remove apenas arquivos temporários
# MANTÉM toda a documentação (.md) e código fonte

echo "🧹 Limpando arquivos temporários do projeto..."
echo ""

# Backup: contar arquivos antes
LOGS_BEFORE=$(find . -maxdepth 1 -name "*.log" | wc -l)
BUILD_SIZE_BEFORE=$(du -sh build 2>/dev/null | cut -f1)

# 1. Remover logs (arquivos .log)
echo "📋 Removendo logs..."
rm -f *.log
echo "  ✓ $(($LOGS_BEFORE)) arquivos .log removidos"

# 2. Remover nohup.out e arquivos .pid
echo "🗑️  Removendo arquivos temporários..."
rm -f nohup.out *.pid
echo "  ✓ Arquivos temporários removidos"

# 3. Limpar build do Gradle (pode ser reconstruído)
echo "🏗️  Limpando diretório build..."
./gradlew clean > /dev/null 2>&1
echo "  ✓ Build limpo (era ${BUILD_SIZE_BEFORE:-0}, agora será reconstruído quando necessário)"

# 4. Remover arquivos temporários diversos
echo "🧽 Removendo outros temporários..."
find . -type f \( -name "*.tmp" -o -name "*.swp" -o -name "*~" -o -name "*.bak" \) -delete
echo "  ✓ Arquivos temporários diversos removidos"

echo ""
echo "✅ Limpeza concluída!"
echo ""
echo "📊 Espaço economizado:"
du -sh . | awk '{print "  Total do projeto: " $1}'
echo ""
echo "📚 MANTIDO (documentação preservada):"
echo "  ✓ Todos os arquivos .md (documentação)"
echo "  ✓ Código fonte (src/)"
echo "  ✓ Configurações do projeto"
echo "  ✓ Scripts (.sh)"
echo ""
echo "🗑️  REMOVIDO (pode ser regenerado):"
echo "  ✓ Logs de execução (*.log)"
echo "  ✓ Build artifacts (build/)"
echo "  ✓ Arquivos temporários"
echo ""
echo "💡 Dica: Execute './gradlew build' quando precisar compilar novamente"
