package gr.aegean.service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.springframework.hateoas.Link;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import gr.aegean.exception.ServerErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetLinkEmail(String recipient, String token) {
        String resetLink = "http://localhost:8080/api/v1/auth/password_reset?token=" + token;

        String body = String.format("""
    <h1>Jarvis password reset</h1>
    <p>We heard that you lost your Jarvis password. Sorry about that!</p>
    <p>But don’t worry! You can use the following button to reset your password:</p>
    <p>
        <a style="background-color: #1a73e8; padding: 10px 20px;
                  color: white; text-decoration:none;font-size:14px;
                  font-family:Roboto,sans-serif;border-radius:5px" href="%s">
        Reset Password
        </a>
    </p>
    <p>If you don’t use this link within 2 hours, it will expire. To get a new password reset link, visit:</p>
    <p>Thanks,</p>
    <p>The Jarvis Team</p>
    """, resetLink);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo("letzasegw@gmail.com");
            helper.setFrom("jarvis.email.from@gmail.com");
            helper.setSubject("Reset your Jarvis password");
            helper.setText(body, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException me) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }


    public void sendPasswordResetConfirmationEmail(String recipient, String username) {
        String resetLink= Link.of("http://localhost:8080/api/v1/auth/password_reset").toString();

        String body = String.format("""
            Hello %s,
                                               
            We wanted to let you know that your Jarvis password was reset.
                                               
            If you did not perform this action, you can recover access by entering %s into the form at %s
                                               
            Please do not reply to this email with your password. We will never ask for your password, and we strongly discourage you from sharing it with anyone.
            
            The Jarvis Team
                    """, username, recipient, resetLink);
        try {
            SimpleMailMessage emailMessage = new SimpleMailMessage();

            emailMessage.setFrom("jarvis.email.from@gmail.com");
            emailMessage.setTo("letzasegw@gmail.com");
            emailMessage.setSubject("Your password was reset");
            emailMessage.setText(body);

            mailSender.send(emailMessage);
        } catch (MailException me) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }

    public void sendEmailUpdateVerificationLinkEmail(String receiver, String username) {
        String body = String.format("""
                Jarvis email verification
                Hello %s,
                            
                Simply click the link below to verify your email address. The link expires in 48 hours.
                            
                You can always visit https://github.com/settings/emails to review email addresses currently associated with your account.
                            
                The Jarvis Team
                        """, username);

        try {
            SimpleMailMessage emailMessage = new SimpleMailMessage();

            emailMessage.setFrom("jarvis.email.from@gmail.com");
            emailMessage.setTo(receiver);
            emailMessage.setSubject("Verify your email address");
            emailMessage.setText(body);

            mailSender.send(emailMessage);
        } catch (MailException me) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }

    public void sendPasswordUpdateNotificationEmail(String receiver, String username) {

    }
}
