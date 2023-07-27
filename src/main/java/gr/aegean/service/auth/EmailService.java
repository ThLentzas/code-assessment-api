package gr.aegean.service.auth;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;

import gr.aegean.exception.ServerErrorException;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private static final String SENDER = "jarvis.email.from@gmail.com";

    public void sendPasswordResetRequestEmail(String recipient, String token) {
        final String resetLink = "http://localhost:8080/api/v1/auth/password_reset?token=" + token;

        Context context = new Context();
        context.setVariable("resetLink", resetLink);
        String emailContent = templateEngine.process("password_reset_request", context);

        sendEmail(recipient, "Reset your Jarvis password", emailContent);
    }

    public void sendPasswordResetConfirmationEmail(String recipient, String username) {
        final String password_reset = "http://localhost:8080/api/v1/auth/password_reset";

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("email", recipient);
        context.setVariable("password_reset", password_reset);
        String emailContent = templateEngine.process("password_reset_success", context);

        sendEmail(recipient, "Your password was reset", emailContent);
    }

    public void sendEmailVerification(String recipient, String username, String token) {
        final String verifyLink = "http://localhost:8080/api/v1/users/settings/email?token=" + token;

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("verifyLink", verifyLink);
        String emailContent = templateEngine.process("email_verification", context);

        sendEmail(recipient, "Verify your email", emailContent);
    }

    private void sendEmail(String recipient, String subject, String emailContent) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(recipient);
            helper.setFrom(SENDER);
            helper.setSubject(subject);
            helper.setText(emailContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException me) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }
}
