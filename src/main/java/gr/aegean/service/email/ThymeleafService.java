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

    public String setPasswordResetEmailContext(String tokenLink, String passwordResetLink) {
        Context context = new Context();
        context.setVariable("tokenLink", tokenLink);
        context.setVariable("passwordResetLink", passwordResetLink);

        return templateEngine.process("password_reset_request", context);
    }

    public String setPasswordResetSuccessEmailContext(String username, String recipient, String passwordResetLink) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("email", recipient);
        context.setVariable("passwordResetLink", passwordResetLink);

        return templateEngine.process("password_reset_success", context);
    }

    public String setEmailVerificationContext(String username, String verifyLink, String accountEmailLink) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("verifyLink", verifyLink);
        context.setVariable("accountEmailLink", accountEmailLink);

        return templateEngine.process("email_verification", context);
    }
}
