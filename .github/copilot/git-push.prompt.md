---
title: Git Commit & Push Automático
description: Analisa mudanças, gera mensagem de commit em português e faz push
---

# Instruções para commit automático

Você deve:

1. **Analisar as mudanças** usando `git status` e `git diff --cached` (ou `git diff` se nada estiver staged)

2. **Gerar uma mensagem de commit** em português seguindo estas regras:
   - Primeira linha: resumo conciso em 50-72 caracteres
   - Linha em branco
   - Corpo: explicação detalhada do que foi mudado e por quê
   - Use verbos no presente: "Adiciona", "Corrige", "Implementa", "Atualiza"
   - Mencione arquivos principais modificados
   - Se houver breaking changes, deixe claro

3. **Executar os comandos**:
   ```bash
   # Se houver arquivos não staged, adicione:
   git add .
   
   # Faça o commit com a mensagem gerada
   git commit -m "<mensagem gerada>"
   
   # Push para o repositório
   git push
   ```

4. **Confirmar** que o push foi bem-sucedido

**Formato da mensagem de commit:**

```
<tipo>: <resumo curto>

<descrição detalhada do que foi feito>

Arquivos modificados:
- arquivo1.java
- arquivo2.md

[Se houver] Breaking changes: <descrição>
```

**Tipos comuns:**
- `feat`: nova funcionalidade
- `fix`: correção de bug
- `docs`: documentação
- `refactor`: refatoração de código
- `perf`: melhoria de performance
- `test`: adição de testes
- `chore`: tarefas de manutenção

---

Execute agora o procedimento completo.
