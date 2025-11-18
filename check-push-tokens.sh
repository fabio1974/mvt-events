#!/bin/bash

echo "🔍 Verificando tokens push registrados..."
echo ""

docker exec -it mvt-events-db psql -U mvt -d mvt-events -c "
SELECT 
    id,
    user_id,
    device_type,
    device_name,
    is_active,
    CASE 
        WHEN token LIKE 'ExponentPushToken%' THEN '✅ TOKEN REAL EXPO'
        WHEN token LIKE 'ExpoToken[DEV_%' THEN '⚠️  TOKEN DESENVOLVIMENTO'
        ELSE '❓ OUTRO TIPO'
    END as token_type,
    substring(token, 1, 60) as token_preview,
    created_at,
    updated_at
FROM user_push_tokens 
WHERE is_active = true 
ORDER BY created_at DESC 
LIMIT 10;
"

echo ""
echo "📊 Resumo:"
docker exec -it mvt-events-db psql -U mvt -d mvt-events -t -c "
SELECT 
    CASE 
        WHEN token LIKE 'ExponentPushToken%' THEN 'Tokens REAIS Expo'
        WHEN token LIKE 'ExpoToken[DEV_%' THEN 'Tokens DESENVOLVIMENTO'
        ELSE 'Outros'
    END as tipo,
    COUNT(*) as quantidade
FROM user_push_tokens 
WHERE is_active = true 
GROUP BY tipo;
"
