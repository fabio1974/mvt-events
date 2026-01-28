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
     * Envia email de recuperacao de senha.
     */
    @Async
    public void sendPasswordResetEmail(User user) {
        if (!isEmailEnabled()) {
            logEmailNotConfigured("reset de senha", user.getUsername(), user.getResetToken(),
                frontendUrl + "/nova-senha?token=" + user.getResetToken());
            return;
        }

        String resetLink = frontendUrl + "/nova-senha?token=" + user.getResetToken();
        String htmlContent = buildPasswordResetEmailHtml(user.getName(), resetLink);
        String subject = "Recuperacao de senha - " + appName;

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
}
