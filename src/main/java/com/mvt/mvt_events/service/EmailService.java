package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Serviço para envio de emails.
 * Suporta confirmação de conta, reset de senha, notificações, etc.
 */
@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    @Value("${spring.mail.username:noreply@zapi10.com}")
    private String fromEmail;

    @Value("${app.name:Zapi10}")
    private String appName;

    /**
     * Envia email de confirmação de conta.
     * O link direciona para o frontend que faz a chamada ao backend.
     */
    @Async
    public void sendConfirmationEmail(User user) {
        if (mailSender == null) {
            log.warn("⚠️ JavaMailSender não configurado. Email de confirmação não enviado para: {}", user.getUsername());
            log.info("📧 Token de confirmação para {}: {}", user.getUsername(), user.getConfirmationToken());
            log.info("🔗 Link de confirmação (backend): {}/api/auth/confirm?token={}", backendUrl, user.getConfirmationToken());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getUsername());
            helper.setSubject("✅ Confirme seu cadastro - " + appName);

            String confirmationLink = frontendUrl + "/confirm-email?token=" + user.getConfirmationToken();
            String directLink = backendUrl + "/api/auth/confirm?token=" + user.getConfirmationToken();

            String htmlContent = buildConfirmationEmailHtml(user.getName(), confirmationLink, directLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ Email de confirmação enviado para: {}", user.getUsername());

        } catch (MessagingException e) {
            log.error("❌ Erro ao enviar email de confirmação para {}: {}", user.getUsername(), e.getMessage());
        }
    }

    /**
     * Constrói o HTML do email de confirmação.
     */
    private String buildConfirmationEmailHtml(String userName, String confirmationLink, String directLink) {
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
                    .button { display: inline-block; background: #667eea; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .button:hover { background: #5a6fd6; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #999; }
                    .link { word-break: break-all; color: #667eea; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚀 %s</h1>
                        <p>Bem-vindo(a)!</p>
                    </div>
                    <div class="content">
                        <h2>Olá, %s!</h2>
                        <p>Obrigado por se cadastrar! Para ativar sua conta e começar a usar nossos serviços, confirme seu email clicando no botão abaixo:</p>
                        
                        <center>
                            <a href="%s" class="button">✅ Confirmar meu email</a>
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
            """.formatted(appName, userName, confirmationLink, directLink, appName);
    }

    /**
     * Reenvia email de confirmação (gera novo token).
     */
    @Async
    public void resendConfirmationEmail(User user, String newToken) {
        user.setConfirmationToken(newToken);
        sendConfirmationEmail(user);
    }
}
