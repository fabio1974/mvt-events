#!/bin/bash

# 📤 Git Push — commit automático em PT-BR, estilo changelog
# Título orientado a domínio (entrega, pagamento, push, etc.) e corpo com:
#   - "Contexto" narrando impacto em produto/API quando detectável
#   - detalhes por camada (service, controller, migration) em linguagem acessível

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
# Nota: usar $( ( ... ) ) e não $(( ... )) — o segundo é aritmética e quebra a lista de arquivos.
FILES_CHANGED=$( ( git diff --name-only; git diff --cached --name-only; git ls-files --others --exclude-standard ) | sort -u | grep -v "^$")
TOTAL_FILES=$(echo "$FILES_CHANGED" | wc -l | xargs)

# Capturar diff completo para analise semantica
FULL_DIFF=$(git diff; git diff --cached)

COMMIT_BODY_FILE="/tmp/git-commit-body-events-$$"
COMMIT_MSG_FILE="/tmp/git-commit-message-events-$$"
> "$COMMIT_BODY_FILE"

# Opcional: mensagem via OpenAI / Anthropic / Ollama (ver scripts/git-commit-ai-lib.sh)
# Por padrão: só heurística (mensagem alinhada ao diff). Para LLM: GIT_COMMIT_AI=1 ./scripts/git-push.sh
export GIT_COMMIT_AI="${GIT_COMMIT_AI:-0}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
USE_AI=0
if [ -f "$SCRIPT_DIR/git-commit-ai-lib.sh" ]; then
  # shellcheck disable=SC1091
  . "$SCRIPT_DIR/git-commit-ai-lib.sh"
  if git_commit_ai_fill "$COMMIT_MSG_FILE" "mvt-events (Spring Boot)" "$FILES_CHANGED" "$FULL_DIFF"; then
    USE_AI=1
    TITLE=$(head -n1 "$COMMIT_MSG_FILE")
    FULL_COMMIT_MESSAGE=$(cat "$COMMIT_MSG_FILE")
  fi
fi

if [ "$USE_AI" != 1 ]; then

# ============================================
# DETECTAR TIPO DE COMMIT
# ============================================

# Palavras no diff que indicam correcao de bug
FIX_SCORE=0
FEAT_SCORE=0
REFACTOR_SCORE=0

# Indicadores de fix no diff
echo "$FULL_DIFF" | grep -qiE "\+.*(Exception|Error|constraint|duplicate|violation|rollback|invalid|npe|nullpointer|NPE|fix|corrige|bug|falha)" && FIX_SCORE=$((FIX_SCORE + 3)) || true
echo "$FULL_DIFF" | grep -qiE -- '-(.*ON CONFLICT|.*upsert|.*deactivat|.*isActive)' && FIX_SCORE=$((FIX_SCORE + 2)) || true
echo "$FULL_DIFF" | grep -qiE "\+.*(ON CONFLICT|upsert|deactivat|isActive)" && FIX_SCORE=$((FIX_SCORE + 2)) || true
echo "$FILES_CHANGED" | grep -qiE "V[0-9]+__cleanup|V[0-9]+__fix|V[0-9]+__drop|V[0-9]+__remove|V[0-9]+__simplify" && FIX_SCORE=$((FIX_SCORE + 2)) || true

# Indicadores de feature nova
echo "$FILES_CHANGED" | grep -qiE "V[0-9]+__add|V[0-9]+__create|V[0-9]+__new" && FEAT_SCORE=$((FEAT_SCORE + 3)) || true
echo "$FULL_DIFF" | grep -qiE "^\+.*(public [A-Z][a-zA-Z]+ [a-z][a-zA-Z]+\(|@PostMapping|@GetMapping|@PutMapping|@DeleteMapping)" && FEAT_SCORE=$((FEAT_SCORE + 2)) || true

# Indicadores de refactor
echo "$FULL_DIFF" | grep -qiE "^\-.*(public|private|protected).*\(" && REFACTOR_SCORE=$((REFACTOR_SCORE + 1)) || true
echo "$FULL_DIFF" | grep -qiE "^\+.*(public|private|protected).*\(" && REFACTOR_SCORE=$((REFACTOR_SCORE + 1)) || true

HAS_SRC=$(echo "$FILES_CHANGED" | grep -c "src/main/java" 2>/dev/null || true); HAS_SRC=${HAS_SRC:-0}
HAS_MIGRATION=$(echo "$FILES_CHANGED" | grep -c "db/migration" 2>/dev/null || true); HAS_MIGRATION=${HAS_MIGRATION:-0}
HAS_BUILD=$(echo "$FILES_CHANGED" | grep -c "build.gradle\|compose\|Dockerfile\|render" 2>/dev/null || true); HAS_BUILD=${HAS_BUILD:-0}
HAS_DOCS=$(echo "$FILES_CHANGED" | grep -c "\.md$\|docs/" 2>/dev/null || true); HAS_DOCS=${HAS_DOCS:-0}
HAS_SCRIPTS=$(echo "$FILES_CHANGED" | grep -c "scripts/\|\.sh$" 2>/dev/null || true); HAS_SCRIPTS=${HAS_SCRIPTS:-0}

if [ "$FIX_SCORE" -gt "$FEAT_SCORE" ] && [ "$FIX_SCORE" -gt 2 ]; then
    COMMIT_TYPE="fix"
elif [ "$HAS_BUILD" -gt 0 ] && [ "$HAS_SRC" -eq 0 ] && [ "$HAS_MIGRATION" -eq 0 ]; then
    COMMIT_TYPE="build"
elif [ "$HAS_DOCS" -gt 0 ] && [ "$HAS_SRC" -eq 0 ] && [ "$HAS_MIGRATION" -eq 0 ]; then
    COMMIT_TYPE="docs"
elif [ "$HAS_SCRIPTS" -gt 0 ] && [ "$HAS_SRC" -eq 0 ] && [ "$HAS_MIGRATION" -eq 0 ]; then
    COMMIT_TYPE="chore"
elif [ "$REFACTOR_SCORE" -gt "$FEAT_SCORE" ] && [ "$FIX_SCORE" -le 2 ]; then
    COMMIT_TYPE="refactor"
else
    COMMIT_TYPE="feat"
fi

# ============================================
# NARRATIVA / CONTEXTO DE FEATURE (backend)
# ============================================
NARRATIVE=""
if echo "$FILES_CHANGED" | grep -q "delivery_stops\|DeliveryStop"; then
    NARRATIVE="${NARRATIVE}• Entregas multi-stop: modelo de paradas, taxa por destino extra ou conclusão/pulo por parada.\n"
fi
if echo "$FULL_DIFF" | grep -qE "planned_route|plannedRoute|PlannedRoute"; then
    NARRATIVE="${NARRATIVE}• Rota planejada: armazena no banco a polyline calculada no app para exibir na corrida sem novas chamadas ao Google.\n"
fi
if echo "$FILES_CHANGED" | grep -q "UserPushToken\|push.token\|push_token"; then
    NARRATIVE="${NARRATIVE}• Push: registro/upsert de token, desativação ou correção de constraints para evitar duplicatas.\n"
fi
if echo "$FULL_DIFF" | grep -qE "skipStop|completeStop|DeliveryStopRepository"; then
    NARRATIVE="${NARRATIVE}• API de paradas: endpoints para marcar entrega ou pular destino individual na mesma corrida.\n"
fi
if echo "$FULL_DIFF" | grep -qE "getPlannedRouteAsGeoJson|plannedRoute"; then
    NARRATIVE="${NARRATIVE}• Tracking: expõe a rota planejada junto com status e posição do motoboy.\n"
fi
if echo "$FILES_CHANGED" | grep -q "SiteConfiguration\|additional_stop\|additionalStop"; then
    NARRATIVE="${NARRATIVE}• Configuração global: taxa adicional por parada ou parâmetros de frete.\n"
fi

# ============================================
# GERAR TITULO
# ============================================

# Nomes das classes Java principais alteradas (sem Config/Test/Application)
CORE_FILES=$(echo "$FILES_CHANGED" | grep "src/main/java" | xargs -I{} basename {} .java 2>/dev/null | \
    grep -v "Config\|Application\|Test" | head -3 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true

# Detectar area principal de mudanca (ignora .cursor/ e git-push-* — evita falso positivo no nome do script)
FILES_FOR_AREA=$(echo "$FILES_CHANGED" | grep -v '^\.cursor/' | grep -vF 'git-push' || true)
MAIN_AREA=""
if echo "$FILES_FOR_AREA" | grep -q "push\|Push\|notification\|Notification"; then
    MAIN_AREA="push notification"
elif echo "$FILES_FOR_AREA" | grep -q "payment\|Payment"; then
    MAIN_AREA="pagamento"
elif echo "$FILES_FOR_AREA" | grep -q "delivery\|Delivery"; then
    MAIN_AREA="entrega"
elif echo "$FILES_FOR_AREA" | grep -q "user\|User"; then
    MAIN_AREA="usuario"
elif echo "$FILES_FOR_AREA" | grep -q "auth\|Auth"; then
    MAIN_AREA="autenticacao"
elif echo "$FILES_FOR_AREA" | grep -q "location\|Location"; then
    MAIN_AREA="localizacao"
fi

# Deteccao de migracao nova
MIGRATION_NAMES=$(echo "$FILES_CHANGED" | grep "db/migration" | \
    sed 's/.*V[0-9]*__//' | sed 's/\.sql$//' | tr '_' ' ' | head -2 | tr '\n' '; ' | sed 's/; $//') || true

# Título enriquecido quando o tema é claro
TITLE_OVERRIDE=""
if echo "$FULL_DIFF" | grep -q "planned_route\|plannedRoute"; then
    TITLE_OVERRIDE="$COMMIT_TYPE(entrega): persiste rota planejada (PostGIS) e devolve no tracking"
elif echo "$FILES_CHANGED" | grep -q "delivery_stops" || echo "$FULL_DIFF" | grep -q "DeliveryStop"; then
    TITLE_OVERRIDE="$COMMIT_TYPE(entrega): paradas múltiplas, API de completar/pular e regras de frete associadas"
elif echo "$FULL_DIFF" | grep -qE "UserPushToken|push.token|duplicate.*token"; then
    TITLE_OVERRIDE="$COMMIT_TYPE(push): ajusta registro de token e consistência no banco"
fi

if [ -n "$TITLE_OVERRIDE" ]; then
    TITLE="$TITLE_OVERRIDE"
elif [ -n "$MAIN_AREA" ] && [ -n "$MIGRATION_NAMES" ]; then
    TITLE="$COMMIT_TYPE($MAIN_AREA): evolução do schema e regras — $MIGRATION_NAMES"
elif [ -n "$MAIN_AREA" ] && [ -n "$CORE_FILES" ]; then
    TITLE="$COMMIT_TYPE($MAIN_AREA): altera $CORE_FILES e comportamento da API relacionado"
elif [ -n "$MIGRATION_NAMES" ]; then
    TITLE="$COMMIT_TYPE: migração e dados — $MIGRATION_NAMES"
elif [ -n "$CORE_FILES" ]; then
    TITLE="$COMMIT_TYPE: atualiza $CORE_FILES (serviços e contratos da API)"
elif [ "$HAS_BUILD" -gt 0 ]; then
    TITLE="$COMMIT_TYPE: atualiza Gradle, dependências ou empacotamento"
elif [ "$HAS_SCRIPTS" -gt 0 ]; then
    TITLE="$COMMIT_TYPE: atualiza scripts de build ou deploy"
else
    TITLE="$COMMIT_TYPE: conjunto de alterações no backend ($TOTAL_FILES arquivo(s))"
fi

# ============================================
# GERAR CORPO EXPLICATIVO
# ============================================

# --- Contexto do que foi removido (o "antes") ---
REMOVED_METHODS=$(echo "$FULL_DIFF" | grep "^-" | grep -v "^---" | \
    grep -oE "(public|private|protected) [^ ]+ [a-z][A-Za-z]+\(" | \
    sed 's/(.*//' | awk '{print $NF}' | sort -u | head -4 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true

ADDED_METHODS=$(echo "$FULL_DIFF" | grep "^+" | grep -v "^+++" | \
    grep -oE "(public|private|protected) [^ ]+ [a-z][A-Za-z]+\(" | \
    sed 's/(.*//' | awk '{print $NF}' | sort -u | head -4 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true

# Detectar mudancas de logica especificas
REMOVED_CALLS=$(echo "$FULL_DIFF" | grep "^-" | grep -v "^---" | \
    grep -oE "[a-z][A-Za-z]+Repository\.[a-z][A-Za-z]+" | head -3 | sort -u | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true

ADDED_CALLS=$(echo "$FULL_DIFF" | grep "^+" | grep -v "^+++" | \
    grep -oE "[a-z][A-Za-z]+Repository\.[a-z][A-Za-z]+" | head -3 | sort -u | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true

# Detectar excecoes/constraints mencionadas no diff
EXCEPTIONS=$(echo "$FULL_DIFF" | grep -oiE "(ConstraintViolation|DataIntegrity|Duplicate|ON CONFLICT|unique constraint|rollback|NullPointer)[A-Za-z]*" | sort -u | head -3 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true

# Queries SQL novas
NEW_QUERIES=$(echo "$FULL_DIFF" | grep "^+" | grep -v "^+++" | \
    grep -oiE "(ON CONFLICT|INSERT INTO|UPDATE .* SET|DELETE FROM|SELECT .* FROM)[^\"]*" | \
    head -2 | sed 's/[[:space:]]\+/ /g' | tr '\n' '; ' | sed 's/; $//') || true

# --- Construir corpo narrativo ---

if [ -n "$NARRATIVE" ]; then
    echo "Contexto (objetivo das mudanças, gerado a partir do diff):" >> "$COMMIT_BODY_FILE"
    printf '%b' "$NARRATIVE" >> "$COMMIT_BODY_FILE"
    echo "" >> "$COMMIT_BODY_FILE"
    echo "" >> "$COMMIT_BODY_FILE"
fi
echo "Detalhes por arquivo ou migration:" >> "$COMMIT_BODY_FILE"

# Bloco 1: contexto das classes alteradas
for file in $FILES_CHANGED; do
    BNAME=$(basename "$file" | sed 's/\.java$//' | sed 's/\.sql$//' | sed 's/\.[^.]*$//')
    FILE_DIFF=$(git diff "$file" 2>/dev/null || git diff --cached "$file" 2>/dev/null || true)

    if [ -z "$FILE_DIFF" ] && [ -f "$file" ]; then
        # Arquivo novo
        case "$file" in
            src/main/resources/db/migration/*)
                MIGRATION_DESC=$(basename "$file" | sed 's/^V[0-9]*__//' | sed 's/\.sql$//' | tr '_' ' ')
                # Tentar extrair contexto do conteudo SQL
                SQL_CONTEXT=$(head -5 "$file" | grep "^--" | sed 's/^-- *//' | head -2 | tr '\n' ' ') || true
                if [ -n "$SQL_CONTEXT" ]; then
                    echo "- Migration $BNAME: $SQL_CONTEXT" >> "$COMMIT_BODY_FILE"
                else
                    echo "- Migration $BNAME: $MIGRATION_DESC" >> "$COMMIT_BODY_FILE"
                fi
                ;;
            *)
                LINES=$(wc -l < "$file" 2>/dev/null | xargs || echo "?")
                echo "- $BNAME: novo arquivo ($LINES linhas)" >> "$COMMIT_BODY_FILE"
                ;;
        esac
        continue
    fi

    [ -z "$FILE_DIFF" ] && continue

    ADDS=$(echo "$FILE_DIFF" | grep "^+" | grep -v "^+++" | wc -l | xargs)
    DELS=$(echo "$FILE_DIFF" | grep "^-" | grep -v "^---" | wc -l | xargs)

    FILE_REMOVED=$(echo "$FILE_DIFF" | grep "^-" | grep -v "^---" | \
        grep -oE "(public|private|protected) [^ ]+ [a-z][A-Za-z]+\(" | \
        sed 's/(.*//' | awk '{print $NF}' | head -2 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true
    FILE_ADDED=$(echo "$FILE_DIFF" | grep "^+" | grep -v "^+++" | \
        grep -oE "(public|private|protected) [^ ]+ [a-z][A-Za-z]+\(" | \
        sed 's/(.*//' | awk '{print $NF}' | head -2 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true

    LINE=""
    case "$file" in
        src/main/java/*/service/*)
            if [ -n "$FILE_REMOVED" ] && [ -n "$FILE_ADDED" ]; then
                LINE="- $BNAME (service): evolui regras de negócio — métodos $FILE_REMOVED em transição para $FILE_ADDED"
            elif [ -n "$FILE_ADDED" ]; then
                LINE="- $BNAME (service): inclui comportamento em $FILE_ADDED (novos fluxos ou validações)"
            elif [ -n "$FILE_REMOVED" ]; then
                LINE="- $BNAME (service): simplifica ou remove trechos em $FILE_REMOVED"
            else
                LINE="- $BNAME (service): ajusta regras de domínio (+$ADDS/-$DELS linhas no diff)"
            fi
            ;;
        src/main/java/*/repository/*)
            if [ -n "$FILE_ADDED" ]; then
                LINE="- $BNAME (repository): novas consultas ou atualizações em banco ($FILE_ADDED)"
            else
                LINE="- $BNAME (repository): refina queries ou mapeamento JPA (+$ADDS/-$DELS linhas)"
            fi
            ;;
        src/main/java/*/controller/*)
            if [ -n "$FILE_ADDED" ]; then
                LINE="- $BNAME (controller): expõe ou altera endpoints REST ($FILE_ADDED)"
            else
                LINE="- $BNAME (controller): ajusta contratos HTTP ou parâmetros (+$ADDS/-$DELS linhas)"
            fi
            ;;
        src/main/java/*/jpa/*|src/main/java/*/entity/*)
            NEW_FIELDS=$(echo "$FILE_DIFF" | grep "^+" | grep -v "^+++" | \
                grep -E "^[+][[:space:]]+private [A-Z]" | sed 's/.*private [^ ]* //' | sed 's/;.*//' | \
                head -3 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true
            if [ -n "$NEW_FIELDS" ]; then
                LINE="- $BNAME (entidade): adiciona campos $NEW_FIELDS"
            else
                LINE="- $BNAME (entidade): ajusta modelo de dados ou relacionamentos (+$ADDS/-$DELS linhas)"
            fi
            ;;
        src/main/resources/db/migration/*)
            MIGRATION_DESC=$(basename "$file" | sed 's/^V[0-9]*__//' | sed 's/\.sql$//' | tr '_' ' ')
            LINE="- Migration $BNAME: $MIGRATION_DESC (evolução de schema alinhada ao domínio)"
            ;;
        build.gradle|settings.gradle)
            NEW_DEPS=$(echo "$FILE_DIFF" | grep "^+" | grep -v "^+++" | \
                grep "implementation\|runtimeOnly\|compileOnly" | \
                sed "s/.*'\([^']*\)'.*/\1/" | head -2 | tr '\n' ', ' | sed 's/,$//' | sed 's/,/, /g') || true
            [ -n "$NEW_DEPS" ] && LINE="- build.gradle: adiciona dependencia $NEW_DEPS" || \
                LINE="- build.gradle: altera configuracao de build"
            ;;
        scripts/*|*.sh)
            LINE="- $(basename "$file"): altera script (+$ADDS/-$DELS linhas)"
            ;;
        *.md|docs/*)
            LINE="- $(basename "$file"): atualiza documentacao"
            ;;
        *)
            LINE="- $BNAME: alterações diversas (+$ADDS/-$DELS linhas no diff)"
            ;;
    esac

    [ -n "$LINE" ] && echo "$LINE" >> "$COMMIT_BODY_FILE"
done

# Bloco 2: nota explicativa sobre o motivo (quando detectavel)
# Só em src/main ou migrations — diff de .sh contém strings tipo ON CONFLICT/grep e vira falso positivo
REASON_LINES=""
APP_DIFF_REASON=0
if [ "$HAS_SRC" -gt 0 ] || [ "$HAS_MIGRATION" -gt 0 ]; then
    APP_DIFF_REASON=1
fi

if [ "$APP_DIFF_REASON" -eq 1 ]; then
if [ "$COMMIT_TYPE" = "fix" ] && [ -n "$EXCEPTIONS" ]; then
    REASON_LINES="Motivo do fix: trata falha ou constraint relacionada a $EXCEPTIONS."
fi

if [ -n "$REMOVED_CALLS" ] && [ -n "$ADDED_CALLS" ] && [ "$REMOVED_CALLS" != "$ADDED_CALLS" ]; then
    REASON_LINES="$REASON_LINES Camada de persistência: chamadas de repositório passam de $REMOVED_CALLS para $ADDED_CALLS."
fi

if [ -n "$NEW_QUERIES" ]; then
    REASON_LINES="$REASON_LINES SQL relevante no diff: $NEW_QUERIES."
fi
fi

# ============================================
# MONTAR MENSAGEM COMPLETA
# ============================================
echo "$TITLE" > "$COMMIT_MSG_FILE"
echo "" >> "$COMMIT_MSG_FILE"
cat "$COMMIT_BODY_FILE" >> "$COMMIT_MSG_FILE"

if [ -n "$REASON_LINES" ]; then
    echo "" >> "$COMMIT_MSG_FILE"
    echo "$REASON_LINES" >> "$COMMIT_MSG_FILE"
fi

FULL_COMMIT_MESSAGE=$(cat "$COMMIT_MSG_FILE")

fi
# ========== fim heurística (USE_AI=0) ==========

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
rm -f "$COMMIT_BODY_FILE" "$COMMIT_MSG_FILE"

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
