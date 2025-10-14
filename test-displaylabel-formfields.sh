#!/bin/bash

# Script para testar se campos com @DisplayLabel aparecem no formFields

echo "🔍 Testando metadata de EventCategory..."
echo ""

# Testa o endpoint de metadata
curl -s http://localhost:8080/api/metadata/eventCategory | jq '{
  formFields: .formFields | map({name, label, type, visible, computed})
}' > /tmp/eventCategory-metadata.json

echo "📋 FormFields retornados:"
cat /tmp/eventCategory-metadata.json | jq '.formFields'

echo ""
echo "🎯 Verificando se campo 'name' (com @DisplayLabel) está presente:"
cat /tmp/eventCategory-metadata.json | jq '.formFields[] | select(.name == "name")'

echo ""
echo "✅ O campo 'name' deve:"
echo "   - Estar presente no array formFields"
echo "   - Ter visible: true"
echo "   - Ter computed: 'categoryName'"
echo ""
