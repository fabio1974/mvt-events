#!/bin/bash

echo "🛑 Parando aplicação na porta 8080..."
echo ""

# 1. Tenta matar via PID file do Gradle
if [ -f app-8080.pid ]; then
    GRADLE_PID=$(cat app-8080.pid)
    echo "📋 PID do Gradle encontrado: $GRADLE_PID"
    
    if ps -p $GRADLE_PID > /dev/null 2>&1; then
        echo "🔫 Matando processo Gradle ($GRADLE_PID)..."
        kill $GRADLE_PID 2>/dev/null
        sleep 1
    else
        echo "⚠️  Processo Gradle ($GRADLE_PID) não está rodando"
    fi
    
    # Remove o PID file
    rm -f app-8080.pid
    echo "🗑️  Arquivo app-8080.pid removido"
else
    echo "⚠️  Arquivo app-8080.pid não encontrado"
fi

echo ""

# 2. Verifica se ainda há processo na porta 8080 (Java do Spring Boot)
PORT_PID=$(lsof -ti:8080 2>/dev/null)

if [ ! -z "$PORT_PID" ]; then
    echo "⚠️  Processo ainda ativo na porta 8080 (PID: $PORT_PID)"
    echo "🔫 Forçando encerramento do processo Java..."
    kill -9 $PORT_PID 2>/dev/null
    sleep 2
    
    # Verifica se realmente parou
    PORT_PID_CHECK=$(lsof -ti:8080 2>/dev/null)
    if [ -z "$PORT_PID_CHECK" ]; then
        echo "✅ Processo finalizado com sucesso"
    else
        echo "❌ Erro: processo ainda ativo!"
        exit 1
    fi
else
    echo "✅ Porta 8080 livre (nenhum processo rodando)"
fi

echo ""
echo "✅ Aplicação parada com sucesso!"
