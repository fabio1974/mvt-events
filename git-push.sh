#!/bin/bash

# 📤 Git Push - Commit automático com mensagem inteligente
# Analisa mudanças e cria commit apropriado para mvt-events (Spring Boot)

set -e
cd "$(dirname "$0")"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📤 GIT PUSH - MVT EVENTS (Backend)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Verificar se há mudanças
if git diff --quiet && git diff --cached --quiet && [ -z "$(git ls-files --others --exclude-standard)" ]; then
    echo "✅ Nenhuma mudança para commit"
    echo ""
    echo "💡 Working directory limpo"
    exit 0
fi

echo "🔍 Analisando mudanças..."
echo ""

# Capturar status do git
git status --short > /tmp/git-status-events.txt

# Contar tipos de mudanças
MODIFIED=$(grep -c "^ M" /tmp/git-status-events.txt 2>/dev/null || echo "0")
ADDED=$(grep -c "^??" /tmp/git-status-events.txt 2>/dev/null || echo "0")
DELETED=$(grep -c "^ D" /tmp/git-status-events.txt 2>/dev/null || echo "0")

# Analisar arquivos modificados para criar mensagem inteligente
FILES_CHANGED=$(git diff --name-only && git diff --cached --name-only && git ls-files --others --exclude-standard)

# Detectar tipo de mudança baseado nos arquivos
COMMIT_TYPE="chore"
COMMIT_SCOPE=""
COMMIT_MESSAGE=""

# Verificar mudanças em controllers
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/controller/"; then
    COMMIT_TYPE="feat"
    COMMIT_SCOPE="api"
    
    CONTROLLER_COUNT=$(echo "$FILES_CHANGED" | grep "controller/" | wc -l | xargs)
    if [ "$CONTROLLER_COUNT" -eq 1 ]; then
        CONTROLLER_NAME=$(echo "$FILES_CHANGED" | grep "controller/" | head -1 | xargs basename | sed 's/Controller\.java$//' | sed 's/\.java$//')
        COMMIT_MESSAGE="update $CONTROLLER_NAME API endpoints"
    else
        COMMIT_MESSAGE="update multiple API endpoints"
    fi
fi

# Verificar mudanças em services
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/service/"; then
    COMMIT_TYPE="feat"
    COMMIT_SCOPE="service"
    
    SERVICE_COUNT=$(echo "$FILES_CHANGED" | grep "service/" | wc -l | xargs)
    if [ "$SERVICE_COUNT" -eq 1 ]; then
        SERVICE_NAME=$(echo "$FILES_CHANGED" | grep "service/" | head -1 | xargs basename | sed 's/Service\.java$//' | sed 's/\.java$//')
        COMMIT_MESSAGE="update $SERVICE_NAME business logic"
    else
        COMMIT_MESSAGE="update business logic"
    fi
fi

# Verificar mudanças em repositories
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/repository/"; then
    COMMIT_TYPE="feat"
    COMMIT_SCOPE="database"
    COMMIT_MESSAGE="update database repositories"
fi

# Verificar mudanças em entities/models
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/entity/\|src/main/java/.*/model/"; then
    COMMIT_TYPE="feat"
    COMMIT_SCOPE="model"
    COMMIT_MESSAGE="update data models and entities"
fi

# Verificar mudanças em DTOs
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/dto/"; then
    COMMIT_TYPE="feat"
    COMMIT_SCOPE="dto"
    COMMIT_MESSAGE="update data transfer objects"
fi

# Verificar mudanças em configuração
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/config/\|application\.properties\|application\.yml"; then
    COMMIT_TYPE="chore"
    COMMIT_SCOPE="config"
    COMMIT_MESSAGE="update application configuration"
fi

# Verificar mudanças em build
if echo "$FILES_CHANGED" | grep -q "pom\.xml\|build\.gradle"; then
    COMMIT_TYPE="build"
    COMMIT_SCOPE="deps"
    COMMIT_MESSAGE="update dependencies"
fi

# Verificar mudanças em scripts
if echo "$FILES_CHANGED" | grep -q "\.sh$"; then
    COMMIT_TYPE="chore"
    COMMIT_SCOPE="scripts"
    
    SCRIPT_COUNT=$(echo "$FILES_CHANGED" | grep "\.sh$" | wc -l | xargs)
    if [ "$SCRIPT_COUNT" -eq 1 ]; then
        SCRIPT_NAME=$(echo "$FILES_CHANGED" | grep "\.sh$" | head -1 | xargs basename)
        COMMIT_MESSAGE="update $SCRIPT_NAME"
    else
        COMMIT_MESSAGE="update deployment scripts"
    fi
fi

# Verificar mudanças em testes
if echo "$FILES_CHANGED" | grep -q "src/test/"; then
    COMMIT_TYPE="test"
    COMMIT_SCOPE=""
    COMMIT_MESSAGE="update tests"
fi

# Verificar mudanças em docs
if echo "$FILES_CHANGED" | grep -q "\.md$\|README"; then
    COMMIT_TYPE="docs"
    COMMIT_SCOPE=""
    COMMIT_MESSAGE="update documentation"
fi

# Verificar correções (palavras-chave em diffs)
if git diff | grep -iq "fix\|bug\|error\|issue\|hotfix"; then
    COMMIT_TYPE="fix"
fi

# Se ainda não temos mensagem específica, criar uma genérica
if [ -z "$COMMIT_MESSAGE" ]; then
    if [ "$MODIFIED" -gt 0 ] && [ "$ADDED" -gt 0 ]; then
        COMMIT_MESSAGE="update and add backend files"
    elif [ "$MODIFIED" -gt 0 ]; then
        COMMIT_MESSAGE="update backend implementation"
    elif [ "$ADDED" -gt 0 ]; then
        COMMIT_MESSAGE="add new backend features"
    elif [ "$DELETED" -gt 0 ]; then
        COMMIT_MESSAGE="remove unused code"
    else
        COMMIT_MESSAGE="update backend"
    fi
fi

# Construir mensagem final
if [ -n "$COMMIT_SCOPE" ]; then
    FINAL_MESSAGE="$COMMIT_TYPE($COMMIT_SCOPE): $COMMIT_MESSAGE"
else
    FINAL_MESSAGE="$COMMIT_TYPE: $COMMIT_MESSAGE"
fi

echo "📊 Estatísticas:"
echo "   Modificados: $MODIFIED"
echo "   Adicionados: $ADDED"
echo "   Removidos: $DELETED"
echo ""

echo "📝 Mensagem de commit gerada:"
echo "   $FINAL_MESSAGE"
echo ""

# Listar arquivos que serão commitados
echo "📁 Arquivos afetados:"
echo "$FILES_CHANGED" | head -10 | sed 's/^/   /'
TOTAL_FILES=$(echo "$FILES_CHANGED" | wc -l | xargs)
if [ "$TOTAL_FILES" -gt 10 ]; then
    echo "   ... e mais $(($TOTAL_FILES - 10)) arquivo(s)"
fi
echo ""

# Verificar branch atual
CURRENT_BRANCH=$(git branch --show-current)
echo "🌿 Branch: $CURRENT_BRANCH"
echo ""

# Git add
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📦 Adicionando arquivos ao stage..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
git add .

echo "✅ Arquivos adicionados"
echo ""

# Git commit
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "💾 Criando commit..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
git commit -m "$FINAL_MESSAGE"

echo "✅ Commit criado"
echo ""

# Git push
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🚀 Fazendo push para $CURRENT_BRANCH..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
git push origin "$CURRENT_BRANCH"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ PUSH CONCLUÍDO COM SUCESSO!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 Resumo:"
echo "   Branch: $CURRENT_BRANCH"
echo "   Commit: $FINAL_MESSAGE"
echo "   Arquivos: $TOTAL_FILES"
echo ""
