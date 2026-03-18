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
MODIFIED=$(grep -c "^ M\|^M" /tmp/git-status-events.txt 2>/dev/null || echo "0")
ADDED=$(grep -c "^??\|^A" /tmp/git-status-events.txt 2>/dev/null || echo "0")
DELETED=$(grep -c "^ D\|^D" /tmp/git-status-events.txt 2>/dev/null || echo "0")

# Analisar arquivos modificados para criar mensagem inteligente
FILES_CHANGED=$(git diff --name-only; git diff --cached --name-only; git ls-files --others --exclude-standard)

# Detectar tipo de mudança baseado nos arquivos (ordem de prioridade)
COMMIT_TYPE="chore"
COMMIT_SCOPE=""
COMMIT_MESSAGE=""
COMMIT_BODY_FILE="/tmp/git-commit-body-events-$$"
> "$COMMIT_BODY_FILE"  # Limpar arquivo

# 1. Verificar mudanças em controllers
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/controller/"; then
    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_TYPE="feat"
        COMMIT_SCOPE="api"
        
        CONTROLLER_COUNT=$(echo "$FILES_CHANGED" | grep -c "controller/" || echo "0")
        if [ "$CONTROLLER_COUNT" -eq 1 ]; then
            CONTROLLER_NAME=$(echo "$FILES_CHANGED" | grep "controller/" | head -1 | xargs basename | sed 's/Controller\.java$//' | sed 's/\.java$//')
            COMMIT_MESSAGE="update $CONTROLLER_NAME API endpoints"
        else
            COMMIT_MESSAGE="update API endpoints ($CONTROLLER_COUNT controllers)"
            
            # Listar controllers modificados (máximo 10)
            echo "$FILES_CHANGED" | grep "controller/" | head -10 | while read -r file; do
                echo "- $(basename "$file" | sed 's/\.java$//')" >> "$COMMIT_BODY_FILE"
            done
        fi
    fi
fi

# 2. Verificar mudanças em services
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/service/"; then
    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_TYPE="feat"
        COMMIT_SCOPE="service"
        
        SERVICE_COUNT=$(echo "$FILES_CHANGED" | grep -c "service/" || echo "0")
        if [ "$SERVICE_COUNT" -eq 1 ]; then
            SERVICE_NAME=$(echo "$FILES_CHANGED" | grep "service/" | head -1 | xargs basename | sed 's/Service\.java$//' | sed 's/\.java$//')
            COMMIT_MESSAGE="update $SERVICE_NAME business logic"
        else
            COMMIT_MESSAGE="update business logic ($SERVICE_COUNT services)"
            
            # Listar services modificados (máximo 10)
            echo "$FILES_CHANGED" | grep "service/" | head -10 | while read -r file; do
                echo "- $(basename "$file" | sed 's/\.java$//')" >> "$COMMIT_BODY_FILE"
            done
        fi
    fi
fi

# 3. Verificar mudanças em repositories
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/repository/"; then
    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_TYPE="feat"
        COMMIT_SCOPE="database"
        
        REPO_COUNT=$(echo "$FILES_CHANGED" | grep -c "repository/" || echo "0")
        COMMIT_MESSAGE="update database repositories ($REPO_COUNT files)"
        
        # Listar repositories modificados (máximo 10)
        echo "$FILES_CHANGED" | grep "repository/" | head -10 | while read -r file; do
            echo "- $(basename "$file" | sed 's/\.java$//')" >> "$COMMIT_BODY_FILE"
        done
    fi
fi

# 4. Verificar mudanças em entities/models
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/entity/\|src/main/java/.*/model/"; then
    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_TYPE="feat"
        COMMIT_SCOPE="model"
        
        MODEL_COUNT=$(echo "$FILES_CHANGED" | grep -cE "entity/|model/" || echo "0")
        COMMIT_MESSAGE="update data models and entities ($MODEL_COUNT files)"
        
        # Listar models/entities modificados (máximo 10)
        echo "$FILES_CHANGED" | grep -E "entity/|model/" | head -10 | while read -r file; do
            echo "- $(basename "$file" | sed 's/\.java$//')" >> "$COMMIT_BODY_FILE"
        done
    fi
fi

# 5. Verificar mudanças em DTOs
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/dto/"; then
    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_TYPE="feat"
        COMMIT_SCOPE="dto"
        
        DTO_COUNT=$(echo "$FILES_CHANGED" | grep -c "dto/" || echo "0")
        COMMIT_MESSAGE="update data transfer objects ($DTO_COUNT DTOs)"
        
        # Listar DTOs modificados (máximo 10)
        echo "$FILES_CHANGED" | grep "dto/" | head -10 | while read -r file; do
            echo "- $(basename "$file" | sed 's/\.java$//')" >> "$COMMIT_BODY_FILE"
        done
    fi
fi

# 6. Verificar mudanças em configuração
if echo "$FILES_CHANGED" | grep -q "src/main/java/.*/config/\|application\.properties\|application\.yml"; then
    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_TYPE="chore"
        COMMIT_SCOPE="config"
        COMMIT_MESSAGE="update application configuration"
        
        echo "$FILES_CHANGED" | grep -E "config/|application\." | while read -r file; do
            echo "- $(basename "$file")" >> "$COMMIT_BODY_FILE"
        done
    fi
fi

# 7. Verificar mudanças em build
if echo "$FILES_CHANGED" | grep -q "pom\.xml\|build\.gradle"; then
    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_TYPE="build"
        COMMIT_SCOPE="deps"
        COMMIT_MESSAGE="update dependencies and build configuration"
        
        echo "$FILES_CHANGED" | grep -E "pom\.xml|build\.gradle" | while read -r file; do
            echo "- $(basename "$file")" >> "$COMMIT_BODY_FILE"
        done
    fi
fi

# 8. Verificar mudanças em scripts
if echo "$FILES_CHANGED" | grep -q "\.sh$"; then
    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_TYPE="chore"
        COMMIT_SCOPE="scripts"
        
        SCRIPT_COUNT=$(echo "$FILES_CHANGED" | grep -c "\.sh$" || echo "0")
        if [ "$SCRIPT_COUNT" -eq 1 ]; then
            SCRIPT_NAME=$(echo "$FILES_CHANGED" | grep "\.sh$" | head -1 | xargs basename)
            COMMIT_MESSAGE="update $SCRIPT_NAME deployment script"
        else
            COMMIT_MESSAGE="update deployment scripts ($SCRIPT_COUNT files)"
            
            # Listar scripts modificados (máximo 10)
            echo "$FILES_CHANGED" | grep "\.sh$" | head -10 | while read -r file; do
                echo "- $(basename "$file")" >> "$COMMIT_BODY_FILE"
            done
        fi
    fi
fi

# 9. Verificar mudanças em testes
if echo "$FILES_CHANGED" | grep -q "src/test/"; then
    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_TYPE="test"
        COMMIT_SCOPE=""
        
        TEST_COUNT=$(echo "$FILES_CHANGED" | grep -c "src/test/" || echo "0")
        COMMIT_MESSAGE="update tests ($TEST_COUNT files)"
        
        # Listar testes modificados (máximo 10)
        echo "$FILES_CHANGED" | grep "src/test/" | head -10 | while read -r file; do
            echo "- $(basename "$file" | sed 's/\.java$//')" >> "$COMMIT_BODY_FILE"
        done
    fi
fi

# 10. Verificar mudanças em docs
if echo "$FILES_CHANGED" | grep -q "\.md$\|README"; then
    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_TYPE="docs"
        COMMIT_SCOPE=""
        
        DOC_COUNT=$(echo "$FILES_CHANGED" | grep -cE "\.md$|README" || echo "0")
        COMMIT_MESSAGE="update documentation ($DOC_COUNT files)"
        
        echo "$FILES_CHANGED" | grep -E "\.md$|README" | head -10 | while read -r file; do
            echo "- $(basename "$file")" >> "$COMMIT_BODY_FILE"
        done
    fi
fi

# 11. Verificar correções (palavras-chave em diffs)
if git diff | grep -iq "fix\|bug\|error\|issue\|hotfix"; then
    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_TYPE="fix"
    fi
fi

# 12. Se ainda não temos mensagem específica, criar uma genérica
if [ -z "$COMMIT_MESSAGE" ]; then
    if [ "$MODIFIED" -gt 0 ] && [ "$ADDED" -gt 0 ]; then
        COMMIT_MESSAGE="update and add backend files"
        echo "- $MODIFIED files modified" >> "$COMMIT_BODY_FILE"
        echo "- $ADDED files added" >> "$COMMIT_BODY_FILE"
    elif [ "$MODIFIED" -gt 0 ]; then
        COMMIT_MESSAGE="update backend implementation ($MODIFIED files)"
    elif [ "$ADDED" -gt 0 ]; then
        COMMIT_MESSAGE="add new backend features ($ADDED files)"
    elif [ "$DELETED" -gt 0 ]; then
        COMMIT_MESSAGE="remove unused code ($DELETED files)"
    else
        COMMIT_MESSAGE="update backend"
    fi
fi

# Construir mensagem final (título)
if [ -n "$COMMIT_SCOPE" ]; then
    FINAL_MESSAGE="$COMMIT_TYPE($COMMIT_SCOPE): $COMMIT_MESSAGE"
else
    FINAL_MESSAGE="$COMMIT_TYPE: $COMMIT_MESSAGE"
fi

# Preparar commit completo (título + corpo se houver)
if [ -s "$COMMIT_BODY_FILE" ]; then
    # Criar arquivo temporário com mensagem completa
    COMMIT_MSG_FILE="/tmp/git-commit-message-events-$$"
    echo "$FINAL_MESSAGE" > "$COMMIT_MSG_FILE"
    echo "" >> "$COMMIT_MSG_FILE"
    cat "$COMMIT_BODY_FILE" >> "$COMMIT_MSG_FILE"
    FULL_COMMIT_MESSAGE=$(cat "$COMMIT_MSG_FILE")
else
    FULL_COMMIT_MESSAGE="$FINAL_MESSAGE"
fi

echo "📊 Estatísticas:"
echo "   Modificados: $MODIFIED"
echo "   Adicionados: $ADDED"
echo "   Removidos: $DELETED"
echo ""

echo "📝 Mensagem de commit gerada:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "$FULL_COMMIT_MESSAGE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
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

# Git commit com mensagem completa (título + corpo)
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "💾 Criando commit..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if [ -s "$COMMIT_BODY_FILE" ]; then
    git commit -F "$COMMIT_MSG_FILE"
else
    git commit -m "$FINAL_MESSAGE"
fi

echo "✅ Commit criado"
echo ""

# Limpar arquivos temporários
rm -f "$COMMIT_BODY_FILE" "$COMMIT_MSG_FILE"

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
