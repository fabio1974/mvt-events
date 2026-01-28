package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.User;
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
 * Serviço para envio de emails via Amazon SES.
 * Suporta confirmação de conta, reset de senha, notificações, etc.
 */
@Service
@Slf4j
public class EmailService {

    @Value("${aws.ses.access-key:}")
    private String awsAccessKey;

    @Value("${aws.ses.secret-key:}")
    private String awsSecretKey;

    @Value("${aws.ses.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.ses.from-email:suporte@zapi10.com.br}")
    private String fromEmail;

    @Value("${aws.ses.from-name:Zapi10}")
    private String fromName;

    @Value("${aws.ses.enabled:true}")
    private boolean sesEnabled;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    @Value("${app.name:Zapi10}")
    private String appName;

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

    /**
     * Envia email de confirmação de conta via Amazon SES.
     * O link direciona para o frontend que faz a chamada ao backend.
     */
    @Async
    public void sendConfirmationEmail(User user) {
        if (!sesEnabled || awsAccessKey == null || awsAccessKey.isBlank() || 
            awsSecretKey == null || awsSecretKey.isBlank()) {
            log.warn("⚠️ Amazon SES não configurado. Email de confirmação não enviado para: {}", user.getUsername());
            log.info("📧 Token de confirmação para {}: {}", user.getUsername(), user.getConfirmationToken());
            log.info("🔗 Link de confirmação (backend): {}/api/auth/confirm?token={}", backendUrl, user.getConfirmationToken());
            return;
        }

        try (SesClient sesClient = createSesClient()) {
            String confirmationLink = frontendUrl + "/confirm-email?token=" + user.getConfirmationToken();
            String htmlContent = buildConfirmationEmailHtml(user.getName(), confirmationLink);
            String subject = "✅ Confirme seu cadastro - " + appName;

            // Formata o remetente com nome
            String formattedFrom = String.format("%s <%s>", fromName, fromEmail);

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .source(formattedFrom)
                    .destination(Destination.builder()
                            .toAddresses(user.getUsername())
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
            log.info("✅ Email de confirmação enviado via Amazon SES para: {} (MessageId: {})", 
                    user.getUsername(), response.messageId());

        } catch (SesException e) {
            log.error("❌ Erro ao enviar email de confirmação via Amazon SES para {}: {}", 
                    user.getUsername(), e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao enviar email via Amazon SES para {}: {}", 
                    user.getUsername(), e.getMessage());
        }
    }

    /**
     * Constrói o HTML do email de confirmação.
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
                        <h2>Olá, %s!</h2>
                        <p>Obrigado por se cadastrar! Para ativar sua conta e começar a usar nossos serviços, confirme seu email clicando no botão abaixo:</p>
                        
                        <center>
                            <a href="%s" class="button" style="display: inline-block; background: #667eea; color: #ffffff !important; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0;">✅ Confirmar meu email</a>
                        </center>
                        
                        <p><small>Se o botão não funcionar, copie e cole este link no navegador:</small></p>
                        <p class="link"><small>%s</small></p>
                        
                        <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                        
                        <p><strong>⚠️ Este link expira em 24 horas.</strong></p>
                        <p>Se você não solicitou este cadastro, ignore este email.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 %s - Todos os direitos reservados</p>
                        <p>Este é um email automático, não responda.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(backendUrl, appName, appName, userName, confirmationLink, confirmationLink, appName);
    }

    /**
     * Reenvia email de confirmação (gera novo token).
     */
    @Async
    public void resendConfirmationEmail(User user, String newToken) {
        user.setConfirmationToken(newToken);
        sendConfirmationEmail(user);
    }

    /**
     * Envia email de recuperação de senha via Amazon SES.
     * O link direciona para o frontend onde o usuário define a nova senha.
     */
    @Async
    public void sendPasswordResetEmail(User user) {
        if (!sesEnabled || awsAccessKey == null || awsAccessKey.isBlank() || 
            awsSecretKey == null || awsSecretKey.isBlank()) {
            log.warn("⚠️ Amazon SES não configurado. Email de reset não enviado para: {}", user.getUsername());
            log.info("📧 Token de reset para {}: {}", user.getUsername(), user.getResetToken());
            log.info("🔗 Link de reset: {}/nova-senha?token={}", frontendUrl, user.getResetToken());
            return;
        }

        try (SesClient sesClient = createSesClient()) {
            String resetLink = frontendUrl + "/nova-senha?token=" + user.getResetToken();
            String htmlContent = buildPasswordResetEmailHtml(user.getName(), resetLink);
            String subject = "🔐 Recuperação de senha - " + appName;

            // Formata o remetente com nome
            String formattedFrom = String.format("%s <%s>", fromName, fromEmail);

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .source(formattedFrom)
                    .destination(Destination.builder()
                            .toAddresses(user.getUsername())
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
            log.info("✅ Email de reset enviado via Amazon SES para: {} (MessageId: {})", 
                    user.getUsername(), response.messageId());

        } catch (SesException e) {
            log.error("❌ Erro ao enviar email de reset via Amazon SES para {}: {}", 
                    user.getUsername(), e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao enviar email de reset via Amazon SES para {}: {}", 
                    user.getUsername(), e.getMessage());
        }
    }

    /**
     * Constrói o HTML do email de recuperação de senha.
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
                        <p>Recuperação de Senha</p>
                    </div>
                    <div class="content">
                        <h2>Olá, %s!</h2>
                        <p>Recebemos uma solicitação para redefinir a senha da sua conta. Clique no botão abaixo para criar uma nova senha:</p>
                        
                        <center>
                            <a href="%s" class="button" style="display: inline-block; background: #667eea; color: #ffffff !important; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0;">🔐 Redefinir minha senha</a>
                        </center>
                        
                        <p><small>Se o botão não funcionar, copie e cole este link no navegador:</small></p>
                        <p class="link"><small>%s</small></p>
                        
                        <div class="warning">
                            <strong>⚠️ Este link expira em 1 hora.</strong><br>
                            Se você não solicitou a recuperação de senha, ignore este email. Sua senha permanecerá inalterada.
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2026 %s - Todos os direitos reservados</p>
                        <p>Este é um email automático, não responda.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(backendUrl, appName, appName, userName, resetLink, resetLink, appName);
    }
}
