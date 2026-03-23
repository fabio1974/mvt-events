#!/usr/bin/env bash
# Biblioteca opcional: mensagem de commit via LLM (OpenAI, Anthropic ou Ollama).
# Uso: . ./git-commit-ai-lib.sh && git_commit_ai_fill "$COMMIT_MSG_FILE" "projeto" "$FILES_CHANGED" "$FULL_DIFF"
#
# Variáveis de ambiente:
#   GIT_COMMIT_AI=0        — desliga IA e usa só heurística do script (mesmo com API key)
#   OPENAI_API_KEY         — prioridade 1; modelo: OPENAI_MODEL (default: gpt-4o-mini)
#   ANTHROPIC_API_KEY      — prioridade 2; modelo: ANTHROPIC_MODEL (default: claude-3-5-haiku-20241022)
#   GIT_COMMIT_USE_OLLAMA=1 ou OLLAMA_HOST — prioridade 3 (local)
#   OLLAMA_HOST            — default http://127.0.0.1:11434
#   OLLAMA_MODEL           — default llama3.2
#
# Requisitos: curl, jq

git_commit_ai_fill() {
  local out_file="$1"
  local project_name="$2"
  local files_changed="$3"
  local full_diff="$4"

  case "${GIT_COMMIT_AI:-1}" in
    0|off|false|OFF) return 1 ;;
  esac

  command -v curl >/dev/null 2>&1 || return 1
  command -v jq >/dev/null 2>&1 || {
    echo "⚠️  IA de commit: instale 'jq' para usar OPENAI/ANTHROPIC/OLLAMA." >&2
    return 1
  }

  local provider=""
  if [ -n "${OPENAI_API_KEY:-}" ]; then provider=openai
  elif [ -n "${ANTHROPIC_API_KEY:-}" ]; then provider=anthropic
  elif [ "${GIT_COMMIT_USE_OLLAMA:-0}" = "1" ] || [ -n "${OLLAMA_HOST:-}" ]; then provider=ollama
  else
    return 1
  fi

  local ollama_base="${OLLAMA_HOST:-http://127.0.0.1:11434}"
  local ollama_model="${OLLAMA_MODEL:-llama3.2}"

  local diff_trunc
  diff_trunc=$(printf '%s' "$full_diff" | head -c 52000)
  if [ "${#full_diff}" -gt 52000 ]; then
    diff_trunc="$diff_trunc

...(diff truncado para caber no contexto do modelo)"
  fi

  local files_trunc
  files_trunc=$(printf '%s' "$files_changed" | head -n 120)

  local prompt_file="/tmp/git-ai-prompt-$$.txt"
  {
    echo "Gera APENAS a mensagem de commit em português (Brasil). Sem markdown, sem cercas \`\`\`, sem aspas envolvendo o texto todo."
    echo ""
    echo "Projeto: $project_name"
    echo ""
    echo "Formato obrigatório:"
    echo "- Linha 1: conventional commit — tipo(escopo): frase clara (feat|fix|chore|docs|build|refactor)"
    echo "- Linha em branco"
    echo "- Corpo: bullets com • explicando impacto em produto/usuário; depois detalhes técnicos se fizer sentido"
    echo "- Não inventes mudanças que não apareçam no diff"
    echo ""
    echo "Arquivos alterados:"
    echo "$files_trunc"
    echo ""
    echo "Diff:"
    echo "$diff_trunc"
  } > "$prompt_file"

  local tmpjson resp body http_code
  tmpjson="/tmp/git-ai-req-$$.json"
  resp="/tmp/git-ai-resp-$$.json"

  set +e
  case "$provider" in
    openai)
      jq -n \
        --arg model "${OPENAI_MODEL:-gpt-4o-mini}" \
        --rawfile content "$prompt_file" \
        '{model:$model,messages:[{role:"user",content:$content}],temperature:0.2,max_tokens:900}' \
        > "$tmpjson"
      http_code=$(curl -sS -o "$resp" -w "%{http_code}" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${OPENAI_API_KEY}" \
        -d @"$tmpjson" \
        "https://api.openai.com/v1/chat/completions")
      body=$(cat "$resp")
      ;;
    anthropic)
      jq -n \
        --arg model "${ANTHROPIC_MODEL:-claude-3-5-haiku-20241022}" \
        --rawfile content "$prompt_file" \
        '{model:$model,max_tokens:1024,messages:[{role:"user",content:$content}]}' \
        > "$tmpjson"
      http_code=$(curl -sS -o "$resp" -w "%{http_code}" \
        -H "Content-Type: application/json" \
        -H "x-api-key: ${ANTHROPIC_API_KEY}" \
        -H "anthropic-version: 2023-06-01" \
        -d @"$tmpjson" \
        "https://api.anthropic.com/v1/messages")
      body=$(cat "$resp")
      ;;
    ollama)
      jq -n \
        --arg model "$ollama_model" \
        --rawfile content "$prompt_file" \
        '{model:$model,messages:[{role:"user",content:$content}],stream:false}' \
        > "$tmpjson"
      http_code=$(curl -sS -o "$resp" -w "%{http_code}" \
        -H "Content-Type: application/json" \
        -d @"$tmpjson" \
        "${ollama_base%/}/api/chat")
      body=$(cat "$resp")
      ;;
  esac
  set -e

  rm -f "$prompt_file" "$tmpjson"

  if [ "$http_code" != "200" ]; then
    echo "⚠️  IA de commit falhou (HTTP $http_code). Trecho: $(echo "$body" | head -c 200)" >&2
    rm -f "$resp"
    return 1
  fi

  local raw=""
  case "$provider" in
    openai)   raw=$(echo "$body" | jq -r '.choices[0].message.content // empty') ;;
    anthropic) raw=$(echo "$body" | jq -r '.content[0].text // empty') ;;
    ollama)   raw=$(echo "$body" | jq -r '.message.content // empty') ;;
  esac
  rm -f "$resp"

  if [ -z "$raw" ] || [ "$raw" = "null" ]; then
    echo "⚠️  IA de commit: resposta vazia ou inválida." >&2
    return 1
  fi

  # Remove cercas markdown se o modelo as colocar
  raw=$(printf '%s' "$raw" | sed '/^```/d')

  printf '%s\n' "$raw" > "$out_file"
  echo "🤖 Mensagem de commit gerada via ${provider} (IA)" >&2
  return 0
}
