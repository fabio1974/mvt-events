package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.User;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

/**
 * Servico para envio de emails.
 * Suporta multiplos provedores: Resend (padrao) e AWS SES.
 * 
 * Para usar Resend: EMAIL_PROVIDER=RESEND (padrao)
 * Para usar AWS SES: EMAIL_PROVIDER=AWS_SES
 */
@Service
@Slf4j
public class EmailService {

    // ============================================================================
    // EMAIL PROVIDER CONFIGURATION
    // ============================================================================
    
    @Value("${email.provider:RESEND}")
    private String emailProvider;

    // ============================================================================
    // RESEND CONFIGURATION
    // ============================================================================

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:suporte@zapi10.com.br}")
    private String resendFromEmail;

    @Value("${resend.from-name:Zapi10}")
    private String resendFromName;

    @Value("${resend.enabled:true}")
    private boolean resendEnabled;

    // ============================================================================
    // AWS SES CONFIGURATION
    // ============================================================================

    @Value("${aws.ses.access-key:}")
    private String awsAccessKey;

    @Value("${aws.ses.secret-key:}")
    private String awsSecretKey;

    @Value("${aws.ses.region:us-east-2}")
    private String awsRegion;

    @Value("${aws.ses.from-email:suporte@zapi10.com.br}")
    private String awsFromEmail;

    @Value("${aws.ses.from-name:Zapi10}")
    private String awsFromName;

    @Value("${aws.ses.enabled:false}")
    private boolean awsSesEnabled;

    // ============================================================================
    // APP CONFIGURATION
    // ============================================================================

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    @Value("${app.name:Zapi10}")
    private String appName;

    // ============================================================================
    // PROVIDER DETECTION
    // ============================================================================

    private boolean isResendProvider() {
        return "RESEND".equalsIgnoreCase(emailProvider);
    }

    private boolean isAwsSesProvider() {
        return "AWS_SES".equalsIgnoreCase(emailProvider);
    }

    private String getFromEmail() {
        return isResendProvider() ? resendFromEmail : awsFromEmail;
    }

    private String getFromName() {
        return isResendProvider() ? resendFromName : awsFromName;
    }

    private boolean isEmailEnabled() {
        if (isResendProvider()) {
            return resendEnabled && resendApiKey != null && !resendApiKey.isBlank();
        } else if (isAwsSesProvider()) {
            return awsSesEnabled && awsAccessKey != null && !awsAccessKey.isBlank() 
                   && awsSecretKey != null && !awsSecretKey.isBlank();
        }
        return false;
    }

    // ============================================================================
    // PUBLIC METHODS
    // ============================================================================

    /**
     * Envia email de confirmacao de conta.
     */
    @Async
    public void sendConfirmationEmail(User user) {
        if (!isEmailEnabled()) {
            logEmailNotConfigured("confirmacao", user.getUsername(), user.getConfirmationToken(),
                frontendUrl + "/confirm-email?token=" + user.getConfirmationToken());
            return;
        }

        String confirmationLink = frontendUrl + "/confirm-email?token=" + user.getConfirmationToken();
        String htmlContent = buildConfirmationEmailHtml(user.getName(), confirmationLink);
        String subject = "Confirme seu cadastro - " + appName;

        sendEmail(user.getUsername(), subject, htmlContent, "confirmacao");
    }

    /**
     * Reenvia email de confirmacao (gera novo token).
     */
    @Async
    public void resendConfirmationEmail(User user, String newToken) {
        user.setConfirmationToken(newToken);
        sendConfirmationEmail(user);
    }

    /**
     * Envia o relatório diário de caixa para o email do client.
     */
    @Async
    public void sendCashReport(User client, com.mvt.mvt_events.dto.CashReportDto report) {
        if (!isEmailEnabled()) {
            log.warn("Email não configurado — relatório de caixa não enviado para {}", client.getUsername());
            return;
        }
        String subject = String.format("Relatório de Caixa — %s — %s",
                client.getName(),
                report.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        String html = buildCashReportHtml(report);
        sendEmail(client.getUsername(), subject, html, "relatório de caixa");
    }

    /**
     * Envia email de recuperacao de senha.
     */
    @Async
    public void sendPasswordResetEmail(User user) {
        log.info("🔵 [DEBUG] sendPasswordResetEmail chamado para: {}", user.getUsername());
        log.info("🔵 [DEBUG] Email enabled: {}", isEmailEnabled());
        log.info("🔵 [DEBUG] Provider: {}", emailProvider);
        log.info("🔵 [DEBUG] Resend API Key presente: {}", resendApiKey != null && !resendApiKey.isBlank());
        
        if (!isEmailEnabled()) {
            logEmailNotConfigured("reset de senha", user.getUsername(), user.getResetToken(),
                frontendUrl + "/nova-senha?token=" + user.getResetToken());
            return;
        }

        String resetLink = frontendUrl + "/nova-senha?token=" + user.getResetToken();
        String htmlContent = buildPasswordResetEmailHtml(user.getName(), resetLink);
        String subject = "Recuperacao de senha - " + appName;

        log.info("🔵 [DEBUG] Chamando sendEmail para: {}", user.getUsername());
        sendEmail(user.getUsername(), subject, htmlContent, "reset de senha");
    }

    // ============================================================================
    // PRIVATE METHODS - EMAIL SENDING
    // ============================================================================

    private void sendEmail(String to, String subject, String htmlContent, String emailType) {
        if (isResendProvider()) {
            sendViaResend(to, subject, htmlContent, emailType);
        } else if (isAwsSesProvider()) {
            sendViaAwsSes(to, subject, htmlContent, emailType);
        } else {
            log.error("Provedor de email nao reconhecido: {}", emailProvider);
        }
    }

    /**
     * Envia email via Resend.
     */
    private void sendViaResend(String to, String subject, String htmlContent, String emailType) {
        try {
            Resend resend = new Resend(resendApiKey);

            String from = String.format("%s <%s>", getFromName(), getFromEmail());

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(from)
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            log.info("Email de {} enviado via Resend para: {} (ID: {})", 
                    emailType, to, response.getId());

        } catch (ResendException e) {
            log.error("Erro ao enviar email de {} via Resend para {}: {}", 
                    emailType, to, e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao enviar email via Resend para {}: {}", 
                    to, e.getMessage());
        }
    }

    /**
     * Envia email via AWS SES.
     */
    private void sendViaAwsSes(String to, String subject, String htmlContent, String emailType) {
        try (SesClient sesClient = createSesClient()) {
            String formattedFrom = String.format("%s <%s>", getFromName(), getFromEmail());

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .source(formattedFrom)
                    .destination(Destination.builder()
                            .toAddresses(to)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .charset("UTF-8")
                                    .data(subject)
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .charset("UTF-8")
                                            .data(htmlContent)
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            log.info("Email de {} enviado via AWS SES para: {} (MessageId: {})", 
                    emailType, to, response.messageId());

        } catch (SesException e) {
            log.error("Erro ao enviar email de {} via AWS SES para {}: {}", 
                    emailType, to, e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao enviar email via AWS SES para {}: {}", 
                    to, e.getMessage());
        }
    }

    /**
     * Cria o cliente SES com as credenciais configuradas.
     */
    private SesClient createSesClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
        return SesClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    private void logEmailNotConfigured(String emailType, String username, String token, String link) {
        String provider = isResendProvider() ? "Resend" : "AWS SES";
        log.warn("{} nao configurado. Email de {} nao enviado para: {}", provider, emailType, username);
        log.info("Token de {} para {}: {}", emailType, username, token);
        log.info("Link de {}: {}", emailType, link);
    }

    // ============================================================================
    // HTML TEMPLATES
    // ============================================================================

    /**
     * Constroi o HTML do email de confirmacao.
     */
    private String buildConfirmationEmailHtml(String userName, String confirmationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; background: #667eea; color: #ffffff !important; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .button:hover { background: #5a6fd6; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #999; }
                    .link { word-break: break-all; color: #667eea; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <img src="%s/new_icon_cropped.png" alt="%s" style="width: 80px; height: 80px; margin-bottom: 10px;" />
                        <h1>%s</h1>
                        <p>Bem-vindo(a)!</p>
                    </div>
                    <div class="content">
                        <h2>Ola, %s!</h2>
                        <p>Obrigado por se cadastrar! Para ativar sua conta e comecar a usar nossos servicos, confirme seu email clicando no botao abaixo:</p>
                        
                        <center>
                            <a href="%s" class="button" style="display: inline-block; background: #667eea; color: #ffffff !important; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0;">Confirmar meu email</a>
                        </center>
                        
                        <p><small>Se o botao nao funcionar, copie e cole este link no navegador:</small></p>
                        <p class="link"><small>%s</small></p>
                        
                        <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                        
                        <p><strong>Este link expira em 24 horas.</strong></p>
                        <p>Se voce nao solicitou este cadastro, ignore este email.</p>
                    </div>
                    <div class="footer">
                        <p>2026 %s - Todos os direitos reservados</p>
                        <p>Este e um email automatico, nao responda.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(backendUrl, appName, appName, userName, confirmationLink, confirmationLink, appName);
    }

    /**
     * Constroi o HTML do email de recuperacao de senha.
     */
    private String buildPasswordResetEmailHtml(String userName, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; background: #667eea; color: #ffffff !important; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .button:hover { background: #5a6fd6; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #999; }
                    .link { word-break: break-all; color: #667eea; }
                    .warning { background: #fff3cd; border: 1px solid #ffc107; padding: 15px; border-radius: 5px; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <img src="%s/new_icon_cropped.png" alt="%s" style="width: 80px; height: 80px; margin-bottom: 10px;" />
                        <h1>%s</h1>
                        <p>Recuperacao de Senha</p>
                    </div>
                    <div class="content">
                        <h2>Ola, %s!</h2>
                        <p>Recebemos uma solicitacao para redefinir a senha da sua conta. Clique no botao abaixo para criar uma nova senha:</p>
                        
                        <center>
                            <a href="%s" class="button" style="display: inline-block; background: #667eea; color: #ffffff !important; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0;">Redefinir minha senha</a>
                        </center>
                        
                        <p><small>Se o botao nao funcionar, copie e cole este link no navegador:</small></p>
                        <p class="link"><small>%s</small></p>
                        
                        <div class="warning">
                            <strong>Este link expira em 1 hora.</strong><br>
                            Se voce nao solicitou a recuperacao de senha, ignore este email. Sua senha permanecera inalterada.
                        </div>
                    </div>
                    <div class="footer">
                        <p>2026 %s - Todos os direitos reservados</p>
                        <p>Este e um email automatico, nao responda.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(backendUrl, appName, appName, userName, resetLink, resetLink, appName);
    }

    /**
     * Constrói o HTML do relatório de caixa.
     */
    private String buildCashReportHtml(com.mvt.mvt_events.dto.CashReportDto r) {
        java.text.NumberFormat brl = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR"));
        java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder channels = new StringBuilder();
        com.mvt.mvt_events.dto.CashReportDto.ChannelSummary balcao = r.getChannels().stream()
                .filter(c -> c.getChannel() == com.mvt.mvt_events.dto.CashReportDto.Channel.BALCAO).findFirst().orElse(null);
        com.mvt.mvt_events.dto.CashReportDto.ChannelSummary mesas = r.getChannels().stream()
                .filter(c -> c.getChannel() == com.mvt.mvt_events.dto.CashReportDto.Channel.MESAS).findFirst().orElse(null);
        com.mvt.mvt_events.dto.CashReportDto.ChannelSummary delivery = r.getChannels().stream()
                .filter(c -> c.getChannel() == com.mvt.mvt_events.dto.CashReportDto.Channel.DELIVERY).findFirst().orElse(null);

        channels.append("<table class=\"data\"><thead><tr>")
                .append("<th>Informação</th><th>Mesas</th><th>Balcão</th><th>Delivery</th><th>Geral</th>")
                .append("</tr></thead><tbody>");
        channels.append(channelRow("Atendimentos",
                String.valueOf(safeCount(mesas)),
                String.valueOf(safeCount(balcao)),
                String.valueOf(safeCount(delivery)),
                String.valueOf(safeCount(mesas) + safeCount(balcao) + safeCount(delivery))));
        channels.append(channelRow("Itens",
                brl.format(safeAmt(mesas != null ? mesas.getItemsTotal() : null)),
                brl.format(safeAmt(balcao != null ? balcao.getItemsTotal() : null)),
                brl.format(safeAmt(delivery != null ? delivery.getItemsTotal() : null)),
                brl.format(safeAmt(mesas != null ? mesas.getItemsTotal() : null)
                        .add(safeAmt(balcao != null ? balcao.getItemsTotal() : null))
                        .add(safeAmt(delivery != null ? delivery.getItemsTotal() : null)))));
        channels.append(channelRow("+Entregas",
                brl.format(java.math.BigDecimal.ZERO),
                brl.format(java.math.BigDecimal.ZERO),
                brl.format(safeAmt(delivery != null ? delivery.getDeliveryFeeTotal() : null)),
                brl.format(safeAmt(delivery != null ? delivery.getDeliveryFeeTotal() : null))));
        channels.append("<tr><td colspan=\"4\" style=\"text-align:right\"><b>TOTAL:</b></td>")
                .append("<td><b>").append(brl.format(r.getGrandTotal())).append("</b></td></tr>");
        channels.append("</tbody></table>");

        StringBuilder pms = new StringBuilder("<table class=\"data\"><thead><tr><th>Forma de Pagamento</th><th>Valor</th></tr></thead><tbody>");
        if (r.getPaymentMethods() == null || r.getPaymentMethods().isEmpty()) {
            pms.append("<tr><td colspan=\"2\" style=\"text-align:center;color:#999\">Sem dados</td></tr>");
        } else {
            for (var e : r.getPaymentMethods().entrySet()) {
                pms.append("<tr><td>").append(pmLabel(e.getKey())).append("</td>")
                   .append("<td>").append(brl.format(e.getValue())).append("</td></tr>");
            }
        }
        pms.append("<tr><td><b>SOMA TOTAL</b></td><td><b>").append(brl.format(r.getGrandTotal())).append("</b></td></tr>");
        pms.append("</tbody></table>");

        StringBuilder items = new StringBuilder("<table class=\"data\"><thead><tr><th>Descrição</th><th>Qnt</th><th>Total</th></tr></thead><tbody>");
        if (r.getItems() == null || r.getItems().isEmpty()) {
            items.append("<tr><td colspan=\"3\" style=\"text-align:center;color:#999\">Sem itens</td></tr>");
        } else {
            for (var it : r.getItems()) {
                items.append("<tr><td>").append(escape(it.getProductName())).append("</td>")
                     .append("<td>").append(it.getQuantity()).append("</td>")
                     .append("<td>").append(brl.format(it.getTotal())).append("</td></tr>");
            }
        }
        items.append("</tbody></table>");

        String cashSection = buildCashSection(r.getCash(), brl);

        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"><style>
              body { font-family: Arial, sans-serif; color:#333; }
              .container { max-width: 760px; margin: 0 auto; padding: 16px; }
              h1 { text-align:center; margin: 4px 0; }
              .sub { text-align:center; color:#666; margin-bottom:16px; }
              h2 { background:#f0f0f0; padding:8px 12px; border-left:4px solid #667eea; margin-top:24px; font-size:14px; }
              table.data { width:100%%; border-collapse:collapse; margin-top:8px; font-size:13px; }
              table.data th, table.data td { border:1px solid #ddd; padding:6px 8px; text-align:left; }
              table.data th { background:#fafafa; }
              .footer { text-align:center; color:#999; font-size:11px; margin-top:24px; }
              .diff-ok { color:#047857; font-weight:bold; }
              .diff-bad { color:#b91c1c; font-weight:bold; }
            </style></head><body>
              <div class="container">
                <h1>CAIXA - %s</h1>
                <div class="sub">%s<br>%s</div>
                %s
                <h2>VENDAS</h2>
                %s
                <h2>CONFERÊNCIA POR FORMA DE PAGAMENTO</h2>
                %s
                <h2>SAÍDA DE ITENS</h2>
                %s
                <div class="footer">Relatório gerado automaticamente por %s.</div>
              </div>
            </body></html>
            """.formatted(
                escape(r.getStoreName()),
                escape(r.getDate().format(df)),
                escape(r.getStoreAddress() != null ? r.getStoreAddress() : ""),
                cashSection,
                channels.toString(),
                pms.toString(),
                items.toString(),
                appName
        );
    }

    private String buildCashSection(com.mvt.mvt_events.dto.CashReportDto.CashSummary c, java.text.NumberFormat brl) {
        if (c == null || "NONE".equals(c.getStatus())) {
            return "<h2>FUNDO DE CAIXA</h2><div style=\"color:#999;font-size:13px;padding:8px\">Nenhuma sessão de caixa registrada para este período.</div>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>FUNDO DE CAIXA</h2>");
        sb.append("<table class=\"data\"><thead><tr>")
          .append("<th>Fundo Inicial</th><th>+ Adições</th><th>− Retiradas/Sangrias</th><th>+ Vendas em Dinheiro</th><th>Saldo Esperado</th>")
          .append("</tr></thead><tbody><tr>")
          .append("<td>").append(brl.format(safeAmt(c.getOpeningBalance()))).append("</td>")
          .append("<td>").append(brl.format(safeAmt(c.getAdditions()))).append("</td>")
          .append("<td>").append(brl.format(safeAmt(c.getWithdrawals()))).append("</td>")
          .append("<td>").append(brl.format(safeAmt(c.getCashSales()))).append("</td>")
          .append("<td><b>").append(brl.format(safeAmt(c.getExpectedBalance()))).append("</b></td>")
          .append("</tr></tbody></table>");

        if (c.getActualBalance() != null) {
            java.math.BigDecimal diff = c.getActualBalance().subtract(safeAmt(c.getExpectedBalance()));
            String cls = diff.signum() == 0 ? "diff-ok" : "diff-bad";
            sb.append("<h2>CONFERÊNCIA DE FECHAMENTO</h2>")
              .append("<table class=\"data\"><tbody>")
              .append("<tr><td>Esperado</td><td>").append(brl.format(safeAmt(c.getExpectedBalance()))).append("</td></tr>")
              .append("<tr><td>Contado</td><td>").append(brl.format(c.getActualBalance())).append("</td></tr>")
              .append("<tr><td><b>Diferença</b></td><td class=\"").append(cls).append("\">")
              .append(brl.format(diff)).append("</td></tr>")
              .append("</tbody></table>");
        } else {
            sb.append("<div style=\"color:#666;font-size:12px;padding:6px\">Caixa ainda em ABERTO — sem conferência de fechamento.</div>");
        }
        return sb.toString();
    }

    private String channelRow(String label, String mesas, String balcao, String delivery, String geral) {
        return "<tr><td>" + label + "</td><td>" + mesas + "</td><td>" + balcao + "</td><td>" + delivery + "</td><td><b>" + geral + "</b></td></tr>";
    }

    private int safeCount(com.mvt.mvt_events.dto.CashReportDto.ChannelSummary c) {
        return c != null ? c.getOrderCount() : 0;
    }

    private java.math.BigDecimal safeAmt(java.math.BigDecimal v) {
        return v != null ? v : java.math.BigDecimal.ZERO;
    }

    private String pmLabel(com.mvt.mvt_events.jpa.PaymentMethod pm) {
        return switch (pm) {
            case CREDIT_CARD -> "Cartão de Crédito";
            case DEBIT_CARD -> "Cartão de Débito";
            case PIX -> "PIX";
            case BANK_SLIP -> "Boleto";
            case CASH -> "Dinheiro";
            case WALLET -> "Carteira Digital";
            case NOT_INFORMED -> "Não Informado";
        };
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
