#!/bin/bash

# 📤 Git Push - Commit automático com mensagem inteligente
# Analisa mudanças e cria commit apropriado para mvt-events (Spring Boot)

set -e
cd "$(dirname "$0")/.."

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
MODIFIED=$(grep -c "^ M\|^M" /tmp/git-status-events.txt 2>/dev/null) || MODIFIED=0
ADDED=$(grep -c "^??\|^A" /tmp/git-status-events.txt 2>/dev/null) || ADDED=0
DELETED=$(grep -c "^ D\|^D" /tmp/git-status-events.txt 2>/dev/null) || DELETED=0

# Analisar arquivos modificados para criar mensagem inteligente
FILES_CHANGED=$(git diff --name-only; git diff --cached --name-only; git ls-files --others --exclude-standard)

# ============================================
# ANÁLISE DETALHADA DAS MUDANÇAS
# ============================================
COMMIT_BODY_FILE="/tmp/git-commit-body-events-$$"
COMMIT_MSG_FILE="/tmp/git-commit-message-events-$$"
> "$COMMIT_BODY_FILE"
TOTAL_FILES=$(echo "$FILES_CHANGED" | grep -v "^$" | wc -l | xargs)

# --- Categorizar TODOS os arquivos modificados ---
AREA_CONTROLLERS=0; AREA_SERVICES=0; AREA_REPOS=0; AREA_ENTITIES=0
AREA_DTOS=0; AREA_CONFIG=0; AREA_MIGRATIONS=0; AREA_BUILD=0
AREA_SCRIPTS=0; AREA_DOCS=0; AREA_INFRA=0
FILE_NAMES=""

for file in $FILES_CHANGED; do
    BNAME=$(basename "$file" | sed 's/\.[^.]*$//')
    FILE_NAMES="$FILE_NAMES $BNAME"
    case "$file" in
        src/main/java/*/controller/*) AREA_CONTROLLERS=$((AREA_CONTROLLERS + 1)) ;;
        src/main/java/*/service/*)    AREA_SERVICES=$((AREA_SERVICES + 1)) ;;
        src/main/java/*/repository/*) AREA_REPOS=$((AREA_REPOS + 1)) ;;
        src/main/java/*/jpa/*|src/main/java/*/entity/*|src/main/java/*/model/*) AREA_ENTITIES=$((AREA_ENTITIES + 1)) ;;
        src/main/java/*/dto/*)        AREA_DTOS=$((AREA_DTOS + 1)) ;;
        src/main/java/*/config/*|*application*.properties|*application*.yml) AREA_CONFIG=$((AREA_CONFIG + 1)) ;;
        src/main/resources/db/migration/*) AREA_MIGRATIONS=$((AREA_MIGRATIONS + 1)) ;;
        build.gradle|settings.gradle|gradle.properties) AREA_BUILD=$((AREA_BUILD + 1)) ;;
        compose*.yaml|compose*.yml|Dockerfile|render.yaml) AREA_INFRA=$((AREA_INFRA + 1)) ;;
        scripts/*|*.sh)               AREA_SCRIPTS=$((AREA_SCRIPTS + 1)) ;;
        *.md|README*|docs/*)          AREA_DOCS=$((AREA_DOCS + 1)) ;;
    esac
done

# Lista de áreas únicas para o título
UNIQUE_AREAS=""
[ "$AREA_CONTROLLERS" -gt 0 ] && UNIQUE_AREAS="${UNIQUE_AREAS}, api"
[ "$AREA_SERVICES" -gt 0 ] && UNIQUE_AREAS="${UNIQUE_AREAS}, services"
[ "$AREA_REPOS" -gt 0 ] && UNIQUE_AREAS="${UNIQUE_AREAS}, repositories"
[ "$AREA_ENTITIES" -gt 0 ] && UNIQUE_AREAS="${UNIQUE_AREAS}, entities"
[ "$AREA_DTOS" -gt 0 ] && UNIQUE_AREAS="${UNIQUE_AREAS}, dtos"
[ "$AREA_MIGRATIONS" -gt 0 ] && UNIQUE_AREAS="${UNIQUE_AREAS}, migrations"
[ "$AREA_BUILD" -gt 0 ] && UNIQUE_AREAS="${UNIQUE_AREAS}, build"
[ "$AREA_INFRA" -gt 0 ] && UNIQUE_AREAS="${UNIQUE_AREAS}, infra"
[ "$AREA_CONFIG" -gt 0 ] && UNIQUE_AREAS="${UNIQUE_AREAS}, config"
[ "$AREA_SCRIPTS" -gt 0 ] && UNIQUE_AREAS="${UNIQUE_AREAS}, scripts"
[ "$AREA_DOCS" -gt 0 ] && UNIQUE_AREAS="${UNIQUE_AREAS}, docs"
UNIQUE_AREAS=$(echo "$UNIQUE_AREAS" | sed 's/^, //')

# --- Determinar tipo do commit ---
if [ "$AREA_CONTROLLERS" -gt 0 ] || [ "$AREA_SERVICES" -gt 0 ] || [ "$AREA_REPOS" -gt 0 ] || [ "$AREA_ENTITIES" -gt 0 ]; then
    COMMIT_TYPE="feat"
elif [ "$AREA_MIGRATIONS" -gt 0 ]; then
    COMMIT_TYPE="feat"
elif [ "$AREA_BUILD" -gt 0 ] || [ "$AREA_INFRA" -gt 0 ]; then
    COMMIT_TYPE="build"
elif [ "$AREA_DOCS" -gt 0 ]; then
    COMMIT_TYPE="docs"
else
    COMMIT_TYPE="chore"
fi

# --- Gerar descrição detalhada por arquivo usando git diff ---
echo "Alterações por arquivo:" >> "$COMMIT_BODY_FILE"
for file in $FILES_CHANGED; do
    BNAME=$(basename "$file" | sed 's/\.java$//' | sed 's/\.sql$//' | sed 's/\.[^.]*$//')

    # Tentar diff de arquivo rastreado (unstaged, depois staged)
    DIFF_OUTPUT=$(git diff "$file" 2>/dev/null)
    if [ -z "$DIFF_OUTPUT" ]; then
        DIFF_OUTPUT=$(git diff --cached "$file" 2>/dev/null)
    fi

    if [ -n "$DIFF_OUTPUT" ]; then
        # Contar adições/remoções
        ADDS=$(echo "$DIFF_OUTPUT" | grep "^+" | grep -v "^+++" | wc -l | xargs)
        DELS=$(echo "$DIFF_OUTPUT" | grep "^-" | grep -v "^---" | wc -l | xargs)

        # Extrair funções/métodos dos hunk headers (@@ ... @@ contexto)
        CONTEXT=$(echo "$DIFF_OUTPUT" | grep "^@@" | sed 's/.*@@[[:space:]]*//' | grep -v "^$" | sed 's/[[:space:]]*{[[:space:]]*$//' | head -3 | tr '\n' '; ' | sed 's/;[[:space:]]*$//')

        if [ -n "$CONTEXT" ]; then
            echo "- $BNAME: $CONTEXT (+$ADDS/-$DELS)" >> "$COMMIT_BODY_FILE"
        else
            echo "- $BNAME (+$ADDS/-$DELS)" >> "$COMMIT_BODY_FILE"
        fi
    else
        # Arquivo novo (untracked)
        if [ -f "$file" ]; then
            LINES=$(wc -l < "$file" | xargs)
            echo "- $BNAME: novo arquivo ($LINES linhas)" >> "$COMMIT_BODY_FILE"
        else
            echo "- $BNAME: novo arquivo" >> "$COMMIT_BODY_FILE"
        fi
    fi
done

# --- Construir título do commit ---
if [ "$TOTAL_FILES" -le 3 ]; then
    NAMES=$(echo "$FILE_NAMES" | xargs | tr ' ' ', ')
    FINAL_MESSAGE="$COMMIT_TYPE: update $NAMES"
else
    FINAL_MESSAGE="$COMMIT_TYPE: update $UNIQUE_AREAS ($TOTAL_FILES arquivos)"
fi

# --- Montar mensagem completa (título + corpo) ---
if [ -s "$COMMIT_BODY_FILE" ]; then
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
