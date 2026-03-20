#!/bin/bash

# 📤 Git Push - Commit automatico com mensagem descritiva em PT-BR
# Analisa git diff e gera mensagem de commit inteligente para mvt-events (Spring Boot)

set -e
cd "$(dirname "$0")/.."

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📤 GIT PUSH - MVT EVENTS (Backend)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Verificar se ha mudancas
if git diff --quiet && git diff --cached --quiet && [ -z "$(git ls-files --others --exclude-standard)" ]; then
    echo "✅ Nenhuma mudanca para commit"
    echo "💡 Working directory limpo"
    exit 0
fi

echo "🔍 Analisando mudancas..."
echo ""

# Capturar arquivos modificados (sem duplicatas)
FILES_CHANGED=$(( git diff --name-only; git diff --cached --name-only; git ls-files --others --exclude-standard ) | sort -u | grep -v "^$")
TOTAL_FILES=$(echo "$FILES_CHANGED" | wc -l | xargs)

# ============================================
# GERAR DESCRICAO POR ARQUIVO
# ============================================
COMMIT_BODY_FILE="/tmp/git-commit-body-events-$$"
COMMIT_MSG_FILE="/tmp/git-commit-message-events-$$"
TITLE_HINTS_FILE="/tmp/git-commit-hints-events-$$"
> "$COMMIT_BODY_FILE"
> "$TITLE_HINTS_FILE"

for file in $FILES_CHANGED; do
    BNAME=$(basename "$file" | sed 's/\.java$//' | sed 's/\.sql$//' | sed 's/\.[^.]*$//')

    # Obter diff do arquivo
    DIFF_OUTPUT=$(git diff "$file" 2>/dev/null)
    if [ -z "$DIFF_OUTPUT" ]; then
        DIFF_OUTPUT=$(git diff --cached "$file" 2>/dev/null)
    fi

    if [ -n "$DIFF_OUTPUT" ]; then
        ADDS=$(echo "$DIFF_OUTPUT" | grep "^+" | grep -v "^+++" | wc -l | xargs)
        DELS=$(echo "$DIFF_OUTPUT" | grep "^-" | grep -v "^---" | wc -l | xargs)

        # Linhas adicionadas (limpas, sem import/comment/blank)
        ADDED_LINES=$(echo "$DIFF_OUTPUT" | grep "^+" | grep -v "^+++" | \
            sed 's/^+//' | sed 's/^[[:space:]]*//' | \
            grep -v "^$" | grep -v "^//" | grep -v "^\*" | grep -v "^import " | \
            grep -v "^@Override" | grep -v "^}$" | grep -v "^{$") || true

        # Detectar o que foi adicionado
        DESCRICAO=""

        # Novos metodos Java
        NEW_METHODS=$(echo "$ADDED_LINES" | grep -oE "(public|private|protected)[^;{]*\(" | sed 's/(.*//' | awk '{print $NF}' | head -3 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true

        # Novos campos
        NEW_FIELDS=$(echo "$ADDED_LINES" | grep -E "^private [A-Z]" | sed 's/private [^ ]* //' | sed 's/;.*//' | head -3 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true

        # Novas dependencias (build.gradle)
        NEW_DEPS=$(echo "$ADDED_LINES" | grep "implementation\|runtimeOnly\|compileOnly" | sed "s/.*'\([^']*\)'.*/\1/" | sed 's/.*"\([^"]*\)".*/\1/' | head -2 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true

        # Determinar verbo
        if [ "$DELS" -eq 0 ] && [ "$ADDS" -gt 0 ]; then
            VERBO="adiciona"
        elif [ "$ADDS" -eq 0 ] && [ "$DELS" -gt 0 ]; then
            VERBO="remove"
        elif [ "$ADDS" -gt "$DELS" ]; then
            VERBO="adiciona"
        else
            VERBO="altera"
        fi

        # Montar descricao por tipo de arquivo
        case "$file" in
            src/main/java/*/controller/*)
                if [ -n "$NEW_METHODS" ]; then
                    DESCRICAO="$VERBO endpoints: $NEW_METHODS"
                else
                    DESCRICAO="$VERBO logica no controller"
                fi
                echo "endpoint" >> "$TITLE_HINTS_FILE"
                ;;
            src/main/java/*/service/*)
                if [ -n "$NEW_METHODS" ]; then
                    DESCRICAO="$VERBO metodos: $NEW_METHODS"
                else
                    DESCRICAO="$VERBO logica de negocio"
                fi
                echo "servico" >> "$TITLE_HINTS_FILE"
                ;;
            src/main/java/*/repository/*)
                if [ -n "$NEW_METHODS" ]; then
                    DESCRICAO="$VERBO queries: $NEW_METHODS"
                else
                    DESCRICAO="$VERBO queries no repositorio"
                fi
                echo "repositorio" >> "$TITLE_HINTS_FILE"
                ;;
            src/main/java/*/jpa/*|src/main/java/*/entity/*|src/main/java/*/model/*)
                if [ -n "$NEW_FIELDS" ]; then
                    DESCRICAO="$VERBO campos: $NEW_FIELDS"
                else
                    DESCRICAO="$VERBO propriedades na entidade"
                fi
                echo "entidade" >> "$TITLE_HINTS_FILE"
                ;;
            src/main/java/*/dto/*)
                if [ -n "$NEW_FIELDS" ]; then
                    DESCRICAO="$VERBO campos: $NEW_FIELDS"
                else
                    DESCRICAO="$VERBO campos no DTO"
                fi
                echo "dto" >> "$TITLE_HINTS_FILE"
                ;;
            src/main/java/*/config/*)
                DESCRICAO="$VERBO configuracao"
                echo "config" >> "$TITLE_HINTS_FILE"
                ;;
            build.gradle|settings.gradle|gradle.properties)
                if [ -n "$NEW_DEPS" ]; then
                    DESCRICAO="$VERBO dependencia: $NEW_DEPS"
                else
                    DESCRICAO="$VERBO configuracao de build"
                fi
                echo "build" >> "$TITLE_HINTS_FILE"
                ;;
            compose*.yaml|compose*.yml|Dockerfile|render.yaml)
                DESCRICAO="$VERBO configuracao de infra/deploy"
                echo "infra" >> "$TITLE_HINTS_FILE"
                ;;
            *application*.properties|*application*.yml)
                DESCRICAO="$VERBO propriedades da aplicacao"
                echo "config" >> "$TITLE_HINTS_FILE"
                ;;
            scripts/*|*.sh)
                DESCRICAO="$VERBO script"
                echo "scripts" >> "$TITLE_HINTS_FILE"
                ;;
            *.md|docs/*)
                DESCRICAO="$VERBO documentacao"
                echo "docs" >> "$TITLE_HINTS_FILE"
                ;;
            *)
                DESCRICAO="$VERBO conteudo (+$ADDS/-$DELS)"
                ;;
        esac

        echo "- $BNAME: $DESCRICAO" >> "$COMMIT_BODY_FILE"
    else
        # Arquivo novo (untracked)
        LINES=0
        [ -f "$file" ] && LINES=$(wc -l < "$file" | xargs)

        case "$file" in
            src/main/resources/db/migration/*)
                MIGRATION_DESC=$(basename "$file" | sed 's/^V[0-9]*__//' | sed 's/\.sql$//' | tr '_' ' ')
                DESCRICAO="nova migracao: $MIGRATION_DESC ($LINES linhas)"
                echo "migracao" >> "$TITLE_HINTS_FILE"
                ;;
            *)
                DESCRICAO="novo arquivo ($LINES linhas)"
                ;;
        esac
        echo "- $BNAME: $DESCRICAO" >> "$COMMIT_BODY_FILE"
    fi
done

# ============================================
# GERAR TITULO DO COMMIT EM PT-BR
# ============================================

HINT_SUMMARY=$(sort "$TITLE_HINTS_FILE" 2>/dev/null | uniq -c | sort -rn | head -1 | awk '{print $2}') || true

# Nomes das classes Java alteradas
CORE_FILES=$(echo "$FILES_CHANGED" | grep "src/main/java" | xargs -I{} basename {} .java 2>/dev/null | \
    grep -v "Config\|Application\|Test" | head -4 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true

# Tipo do commit
HAS_SRC=$(echo "$FILES_CHANGED" | grep -c "src/main/java" 2>/dev/null) || HAS_SRC=0
HAS_MIGRATION=$(echo "$FILES_CHANGED" | grep -c "db/migration" 2>/dev/null) || HAS_MIGRATION=0
HAS_BUILD=$(echo "$FILES_CHANGED" | grep -c "build.gradle\|compose\|Dockerfile\|render" 2>/dev/null) || HAS_BUILD=0
HAS_DOCS=$(echo "$FILES_CHANGED" | grep -c "\.md$\|docs/" 2>/dev/null) || HAS_DOCS=0

if [ "$HAS_SRC" -gt 0 ] || [ "$HAS_MIGRATION" -gt 0 ]; then
    COMMIT_TYPE="feat"
elif [ "$HAS_BUILD" -gt 0 ]; then
    COMMIT_TYPE="build"
elif [ "$HAS_DOCS" -gt 0 ] && [ "$HAS_SRC" -eq 0 ]; then
    COMMIT_TYPE="docs"
else
    COMMIT_TYPE="chore"
fi

# Titulo descritivo em PT-BR
if [ -n "$CORE_FILES" ]; then
    NUM_CORE=$(echo "$CORE_FILES" | tr ',' '\n' | wc -l | xargs)
    if [ "$NUM_CORE" -le 3 ]; then
        TITLE="$COMMIT_TYPE: atualiza $CORE_FILES"
    else
        case "$HINT_SUMMARY" in
            endpoint)    TITLE="$COMMIT_TYPE: atualiza endpoints e logica de negocio" ;;
            servico)     TITLE="$COMMIT_TYPE: atualiza servicos e logica de negocio" ;;
            entidade)    TITLE="$COMMIT_TYPE: atualiza entidades e modelo de dados" ;;
            repositorio) TITLE="$COMMIT_TYPE: atualiza repositorios e queries" ;;
            migracao)    TITLE="$COMMIT_TYPE: atualiza modelo de dados com migracao" ;;
            *)           TITLE="$COMMIT_TYPE: atualiza $CORE_FILES" ;;
        esac
    fi
    if [ "$HAS_MIGRATION" -gt 0 ] && ! echo "$TITLE" | grep -qi "migra"; then
        TITLE="$TITLE com migracao"
    fi
else
    if [ "$HAS_BUILD" -gt 0 ]; then
        TITLE="$COMMIT_TYPE: atualiza configuracao de build"
    elif [ "$HAS_DOCS" -gt 0 ]; then
        TITLE="$COMMIT_TYPE: atualiza documentacao"
    else
        TITLE="$COMMIT_TYPE: atualiza scripts"
    fi
fi

# ============================================
# MONTAR MENSAGEM COMPLETA
# ============================================
echo "$TITLE" > "$COMMIT_MSG_FILE"
echo "" >> "$COMMIT_MSG_FILE"
cat "$COMMIT_BODY_FILE" >> "$COMMIT_MSG_FILE"
FULL_COMMIT_MESSAGE=$(cat "$COMMIT_MSG_FILE")

# ============================================
# EXIBIR E EXECUTAR
# ============================================
echo "📊 Estatisticas: $TOTAL_FILES arquivo(s)"
echo ""
echo "📝 Mensagem de commit:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "$FULL_COMMIT_MESSAGE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "📁 Arquivos:"
echo "$FILES_CHANGED" | sed 's/^/   /'
echo ""

CURRENT_BRANCH=$(git branch --show-current)
echo "🌿 Branch: $CURRENT_BRANCH"
echo ""

echo "📦 Adicionando ao stage..."
git add .
echo "✅ Stage pronto"
echo ""

echo "💾 Criando commit..."
git commit -F "$COMMIT_MSG_FILE"
echo "✅ Commit criado"
echo ""

# Limpar temporarios
rm -f "$COMMIT_BODY_FILE" "$COMMIT_MSG_FILE" "$TITLE_HINTS_FILE"

echo "🚀 Push para $CURRENT_BRANCH..."
git push origin "$CURRENT_BRANCH"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ PUSH CONCLUIDO!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "   Branch: $CURRENT_BRANCH"
echo "   Commit: $TITLE"
echo "   Arquivos: $TOTAL_FILES"
echo ""
