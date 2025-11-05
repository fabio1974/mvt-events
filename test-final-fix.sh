#!/bin/bash

echo ""
echo "=========================================="
echo "  🔧 Testando Correção Final do ConcurrentModificationException"
echo "=========================================="
echo ""

echo "⏳ Aguardando recompilação automática..."
sleep 15

echo "✅ Testando endpoint..."
echo ""

curl -s 'http://localhost:8080/api/organizations?page=0&size=10' \
  -H 'Accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJvcmdhbml6YXRpb25JZCI6Niwicm9sZSI6IkFETUlOIiwiYWRkcmVzcyI6IlJ1YSBKb2FxdWltIE5hYnVjbyBQZXJlaXJhLCAxMTYiLCJwaG9uZSI6Ijg1OTk3NTcyOTE5IiwibmFtZSI6IkZhYmlvIEFkbWluIiwiY3BmIjoiMjIyLjMzMy40NDQtMDUiLCJ1c2VySWQiOiI3NDJmNThlYS01YmMxLTRiYjUtODRkYy01ZWE0NjNkMTUwNDQiLCJhdXRob3JpdGllcyI6WyJST0xFX0FETUlOIl0sImVtYWlsIjoiYWRtaW5AdGVzdC5jb20iLCJzdWIiOiJhZG1pbkB0ZXN0LmNvbSIsImlhdCI6MTc2MTQxOTQzMiwiZXhwIjoxNzYxNDM3NDMyfQ.E0TYyoOAqjTk3KDsCNhGrIbMi_-iOQSaY9zXooQYQ58' | jq '.content[0] | {id, name, employmentContracts: (.employmentContracts | length), serviceContracts: (.serviceContracts | length)}'

echo ""
echo "=========================================="
