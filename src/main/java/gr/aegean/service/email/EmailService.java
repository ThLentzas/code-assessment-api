package gr.aegean.service.email;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;

import gr.aegean.exception.ServerErrorException;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final ThymeleafService thymeleafService;
    private static final String SENDER = "jarvis.email.from@gmail.com";

    public void sendPasswordResetEmail(String recipient, String token) {
        final String resetLink = "http://localhost:8080/api/v1/auth/password_reset?token=" + token;
        String emailContent = thymeleafService.setPasswordResetEmailContent(resetLink);

        sendEmail(recipient, "Reset your Jarvis password", emailContent);
    }

    public void sendPasswordResetSuccessEmail(String recipient, String username) {
        final String password_reset = "http://localhost:8080/api/v1/auth/password_reset";
        String emailContent = thymeleafService.setPasswordResetSuccessEmailContent(username, recipient, password_reset);

        sendEmail(recipient, "Your password was reset", emailContent);
    }

    public void sendEmailVerification(String recipient, String username, String token) {
        final String verifyLink = "http://localhost:8080/api/v1/users/settings/email?token=" + token;
        String emailContent = thymeleafService.setEmailVerificationContent(username, verifyLink);

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
