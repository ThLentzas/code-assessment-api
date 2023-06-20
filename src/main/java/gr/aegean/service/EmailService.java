package gr.aegean.service;

import gr.aegean.exception.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;
    public void sendPasswordResetEmail(String receiver, String token) {
        String resetLink= Link.of(
                "http://localhost:8080/api/v1/auth/password_reset?token=" + token).toString();

        try {
            String body = String.format("""
        Jarvis password reset
        \n
        We heard that you lost your Jarvis password. Sorry about that!
        \n
        But don’t worry! You can use the following button to reset your password:
        \n
        <a href="%s">Reset Password</a>
        \n
        If you don’t use this link within 2 hours, it will expire. To get a new password reset link, visit:
        \n
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
}
