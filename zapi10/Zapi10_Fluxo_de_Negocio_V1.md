# 🚦 Zapi10 – Fluxo de Negócio (Versão Numerada)

Este documento descreve o **fluxo oficial de operação do Zapi10**, conforme o diagrama criado no Figma.  
O processo envolve as principais entidades: **Cliente**, **Plataforma Zapi10**, **ADM (Gerente Local)**, **Motoboy** e **Sistema de Pagamentos**.

---

## 🔢 Etapas do Fluxo

### 1️⃣ Cliente faz pedido de entrega
O usuário (cliente) acessa o aplicativo Zapi10 e cria um novo pedido de entrega informando **origem**, **destino**, **tipo de entrega** e **valor estimado**.

### 2️⃣ Plataforma Zapi10 recebe o pedido
O backend da plataforma recebe o pedido e valida as informações (autenticação, saldo, área de cobertura etc.).

### 3️⃣ Identificação da região e do ADM responsável
O sistema identifica automaticamente **qual ADM (Gerente Local)** é responsável pelo cliente

### 4️⃣ Publicação do pedido aos motoboys
O ADM é notificado e pode **atribuir manualmente** a entrega a um motoboy específico **ou** permitir que o pedido fique **disponível para aceitação voluntária** pelos motoboys cadastrados naquela região.

### 5️⃣ Motoboy aceita o pedido
Um motoboy disponível **aceita a entrega** através do aplicativo, assumindo a responsabilidade pelo pedido.

### 6️⃣ Plataforma confirma o pedido ao cliente
O sistema confirma ao cliente que o pedido foi aceito, exibindo o nome e os dados do motoboy (placa, tempo estimado etc.).

### 7️⃣ Coleta no ponto de origem
O motoboy se desloca até o **ponto de retirada** (origem) informado no pedido.

### 8️⃣ Entrega no destino
O motoboy realiza a **entrega ao destinatário**, finalizando a corrida.

### 9️⃣ Atualização de status e cálculo de valores
A plataforma atualiza o status da entrega para **“concluída”** e processa automaticamente o cálculo dos repasses:
- 85% → Motoboy  
- 5% → ADM (Gerente Local)  
- 10% → Plataforma Zapi10

### 🔟 Processamento financeiro
O módulo financeiro registra o pagamento, gera comprovantes e atualiza saldos individuais.

### 11️⃣ Geração de relatórios e feedback
A plataforma disponibiliza os **relatórios de entrega e repasses**, tanto para o ADM quanto para o motoboy e o cliente.

### 12️⃣ Avaliação do serviço
O cliente é convidado a **avaliar a entrega** (nota + comentário), contribuindo para o ranking de desempenho dos motoboys.

---

## 🧭 Fluxo Resumido

1. Cliente → Cria pedido  
2. Plataforma → Recebe e valida  
3. Plataforma → Identifica ADM  
4. ADM → Publica aos motoboys  
5. Motoboy → Aceita entrega  
6. Plataforma → Confirma ao cliente  
7. Motoboy → Coleta na origem  
8. Motoboy → Entrega no destino  
9. Plataforma → Calcula valores  
10. Pagamento → Distribui repasses  
11. Plataforma → Gera relatório  
12. Cliente → Avalia o serviço  

---

**Este fluxo é o processo operacional oficial do sistema Zapi10**, integrando as camadas **Operacional**, **Financeira** e **Institucional** do projeto.
