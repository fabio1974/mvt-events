#!/bin/bash

echo "🚀 Iniciando aplicação na porta 8080..."
echo ""

# Verifica se a porta 8080 está em uso
PORT_PID=$(lsof -ti:8080)

if [ ! -z "$PORT_PID" ]; then
    echo "⚠️  Porta 8080 ocupada pelo processo $PORT_PID"
    echo "🔫 Matando processo..."
    kill -9 $PORT_PID
    sleep 2
    echo "✅ Processo finalizado"
    echo ""
fi

# Carrega variáveis de ambiente do .env.local
if [ -f .env.local ]; then
    echo "📦 Carregando variáveis de .env.local..."
    set -a
    source .env.local
    set +a
    echo "✅ Variáveis carregadas"
    echo ""
else
    echo "⚠️  Arquivo .env.local não encontrado!"
    echo ""
fi

# Limpa log anterior
> bootrun-8080.log

# Inicia a aplicação com nohup
echo "🏃 Iniciando Spring Boot..."
echo "⏳ Aguarde, isso pode levar 30-60 segundos..."
echo ""

nohup ./gradlew bootRun --args='--server.port=8080' > bootrun-8080.log 2>&1 &

# Salva o PID do shell script (não do Java)
GRADLE_PID=$!
echo $GRADLE_PID > app-8080.pid

echo "📋 Gradle PID: $GRADLE_PID"
echo ""

# Aguarda o log começar a aparecer
echo "⏳ Aguardando inicialização..."
sleep 5

# Mostra as primeiras linhas do log
if [ -s bootrun-8080.log ]; then
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📄 Primeiras linhas do log:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    head -15 bootrun-8080.log
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
else
    echo "⚠️  Log ainda não iniciou. Aguarde mais alguns segundos."
fi

echo ""
echo "✅ Aplicação está INICIANDO (não espere ela subir completamente)"
echo ""
echo "📊 Comandos úteis:"
echo "   Ver log completo: tail -f bootrun-8080.log"
echo "   Health check:     ./health-check.sh"
echo "   Parar aplicação:  kill \$(cat app-8080.pid)"
echo ""
