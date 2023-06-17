package gr.aegean.service;

import org.apache.commons.validator.routines.EmailValidator;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import gr.aegean.model.user.User;
import gr.aegean.model.user.UserPrincipal;
import gr.aegean.security.auth.AuthResponse;
import gr.aegean.security.auth.RegisterRequest;

import gr.aegean.exception.BadCredentialsException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        User user = new User(
                request.firstname(),
                request.lastname(),
                request.username(),
                request.email(),
                request.password(),
                request.bio(),
                request.location(),
                request.company());

        validateUser(user);
        user.setPassword(encoder.encode(user.getPassword()));
        UserPrincipal userPrincipal = new UserPrincipal(user);

        Integer id = userService.registerUser(user);
        String jwtToken = jwtService.assignToken(userPrincipal);

        return new AuthResponse(jwtToken, id);
    }

    private void validateUser(User user) {
        validateName(user.getFirstname(), user.getLastname(), user.getUsername());
        validateEmail(user.getEmail());
        validatePassword(user.getPassword());
    }

    private void validateName(String firstname, String lastname, String username) {
        if (firstname.length() > 30 || !firstname.matches("^[a-zA-Z]*$")) {
            throw new BadCredentialsException(
                    "Invalid firstname. Name should contain only characters and can't be more than 30 characters long");
        }

        if (lastname.length() > 30 || !lastname.matches("^[a-zA-Z]*$")) {
            throw new BadCredentialsException(
                    "Invalid lastname. Name should contain only characters and can't be more than 30 characters long");
        }

        if(username.length() > 30) {
            throw new BadCredentialsException(
                    "Invalid username. Username can't be more than 30 characters long");
        }
    }

    /**
     * @throws BadCredentialsException if the email does not match the email pattern.
     */
    private void validateEmail(String email) {
        EmailValidator validator = EmailValidator.getInstance();

        if (!validator.isValid(email)) {
            throw new BadCredentialsException("Invalid email");
        }
    }

    private void validatePassword(String password) {
        PasswordValidator validator = new PasswordValidator(
                new LengthRule(8, 30),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1)
        );

        RuleResult result = validator.validate(new PasswordData(password));
        if (!result.isValid()) {
            throw new BadCredentialsException(validator.getMessages(result).get(0));
        }
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request.firstname() == null || request.firstname().isEmpty()
                || request.lastname() == null || request.lastname().isEmpty()
                || request.username() == null || request.username().isEmpty()
                || request.email() == null || request.email().isEmpty()
                || request.password() == null || request.password().isEmpty()) {
            throw new BadCredentialsException("Fields are necessary");
        }
    }
}
