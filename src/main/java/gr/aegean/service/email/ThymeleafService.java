package gr.aegean.service.email;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import lombok.RequiredArgsConstructor;


/*
    Responsible for setting the content of the emails. Initially the process was done by email service but we
    decoupled it.
 */
@Service
@RequiredArgsConstructor
public class ThymeleafService {
    private final TemplateEngine templateEngine;

    public String setPasswordResetEmailContent(String resetLink) {
        Context context = new Context();
        context.setVariable("resetLink", resetLink);

        return templateEngine.process("password_reset_request", context);
    }

    public String setPasswordResetSuccessEmailContent(String username, String recipient, String password_reset) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("email", recipient);
        context.setVariable("password_reset", password_reset);

        return templateEngine.process("password_reset_success", context);
    }

    public String setEmailVerificationContent(String username, String verifyLink) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("verifyLink", verifyLink);

        return templateEngine.process("email_verification", context);
    }
}
