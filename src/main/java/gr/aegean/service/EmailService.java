package gr.aegean.service;

import org.springframework.hateoas.Link;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import gr.aegean.exception.ServerErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetLinkEmail(String receiver, String token) {
        String resetLink= Link.of("http://localhost:8080/api/v1/auth/password_reset?token=" + token).toString();

        try {
            String body = String.format("""
        Jarvis password reset
        
        We heard that you lost your Jarvis password. Sorry about that!

        But don’t worry! You can use the following button to reset your password:

        <a href="%s">Reset Password</a>

        If you don’t use this link within 2 hours, it will expire. To get a new password reset link, visit:
        
        Thanks,
        The Jarvis Team
        """, resetLink);

            SimpleMailMessage emailMessage = new SimpleMailMessage();

            emailMessage.setFrom("jarvis.email.from@gmail.com");
            emailMessage.setTo("letzasegw@gmail.com");
            emailMessage.setSubject("Reset your Jarvis password");
            emailMessage.setText(body);

            mailSender.send(emailMessage);
        } catch (MailException me) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }

    public void sendPasswordResetConfirmationEmail(String receiver, String username) {
        String resetLink= Link.of("http://localhost:8080/api/v1/auth/password_reset").toString();

        String body = String.format("""
            Hello %s,
                                               
            We wanted to let you know that your Jarvis password was reset.
                                               
            If you did not perform this action, you can recover access by entering %s into the form at %s
                                               
            Please do not reply to this email with your password. We will never ask for your password, and we strongly discourage you from sharing it with anyone.
            
            The Jarvis Team
                    """, username, receiver, resetLink);
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
}
