#!/bin/bash

# Script para subir a aplicação Zapi10

echo "====================================="
echo "Subindo Zapi10..."
echo "====================================="

cd /Users/jose.barros.br/Documents/projects/mvt-events

# Limpar build anterior
echo "Limpando build anterior..."
./gradlew clean

# Compilar
echo "Compilando..."
./gradlew compileJava

# Subir aplicação
echo "Iniciando aplicação..."
./gradlew bootRun

