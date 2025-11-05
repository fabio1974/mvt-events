#!/bin/bash

echo "🛑 Parando aplicação..."
pkill -9 -f "mvt-events"
pkill -9 -f "gradle"
sleep 2

echo "🧹 Limpando build anterior..."
cd /Users/jose.barros.br/Documents/projects/mvt-events
./gradlew clean

echo "🔨 Compilando código atualizado..."
./gradlew compileJava

if [ $? -eq 0 ]; then
    echo "✅ Compilação bem-sucedida!"
    echo ""
    echo "🚀 Iniciando aplicação..."
    ./gradlew bootRun &
    
    echo "⏳ Aguardando 20 segundos para inicialização..."
    sleep 20
    
    echo ""
    echo "🧪 Testando endpoint..."
    curl -s 'http://localhost:8080/api/organizations?page=0&size=10' \
      -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJvcmdhbml6YXRpb25JZCI6Niwicm9sZSI6IkFETUlOIiwiYWRkcmVzcyI6IlJ1YSBKb2FxdWltIE5hYnVjbyBQZXJlaXJhLCAxMTYiLCJwaG9uZSI6Ijg1OTk3NTcyOTE5IiwibmFtZSI6IkZhYmlvIEFkbWluIiwiY3BmIjoiMjIyLjMzMy40NDQtMDUiLCJ1c2VySWQiOiI3NDJmNThlYS01YmMxLTRiYjUtODRkYy01ZWE0NjNkMTUwNDQiLCJhdXRob3JpdGllcyI6WyJST0xFX0FETUlOIl0sImVtYWlsIjoiYWRtaW5AdGVzdC5jb20iLCJzdWIiOiJhZG1pbkB0ZXN0LmNvbSIsImlhdCI6MTc2MTQxOTQzMiwiZXhwIjoxNzYxNDM3NDMyfQ.E0TYyoOAqjTk3KDsCNhGrIbMi_-iOQSaY9zXooQYQ58' | jq '.'
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "🎉 SUCESSO! Endpoint funcionando!"
    else
        echo ""
        echo "❌ Ainda há erro. Verifique os logs acima."
    fi
else
    echo "❌ Erro na compilação!"
fi
