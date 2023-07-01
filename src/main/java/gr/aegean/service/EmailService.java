package gr.aegean.service;

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

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(recipient);
            helper.setFrom(SENDER);
            helper.setSubject("Reset your Jarvis password");
            helper.setText(emailContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException me) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }

    public void sendPasswordResetConfirmationEmail(String recipient, String username) {
        final String password_reset = "http://localhost:8080/api/v1/auth/password_reset";

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("email", recipient);
        context.setVariable("password_reset", password_reset);
        String emailContent = templateEngine.process("password_reset_success", context);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(recipient);
            helper.setFrom(SENDER);
            helper.setSubject("Your password was reset");
            helper.setText(emailContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException me) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }

    public void sendEmailVerification(String recipient, String username, String token) {
        final String verifyLink = "http://localhost:8080/api/v1/users/settings/email?token=" + token;

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("verifyLink", verifyLink);
        String emailContent = templateEngine.process("email_verification", context);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(recipient);
            helper.setFrom(SENDER);
            helper.setSubject("Verify your email");
            helper.setText(emailContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException me) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }
}
