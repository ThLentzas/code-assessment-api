package gr.aegean.service;

import gr.aegean.exception.UnauthorizedException;
import gr.aegean.security.auth.AuthRequest;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

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
        user.setPassword(passwordEncoder.encode(request.password()));
        UserPrincipal userPrincipal = new UserPrincipal(user);

        Integer id = userService.registerUser(user);
        String jwtToken = jwtService.assignToken(userPrincipal);

        return new AuthResponse(jwtToken, id);
    }

    public AuthResponse authenticate(AuthRequest request) {
        validateAuthRequest(request);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (Exception e) {
            throw new UnauthorizedException("Username or password is incorrect");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String jwtToken = jwtService.assignToken(userPrincipal);

        return new AuthResponse(jwtToken);
    }

    private void validateUser(User user) {
        validateName(user.getFirstname(), user.getLastname(), user.getUsername());
        validateEmail(user.getEmail());
        validatePassword(user.getPassword());
        validateLocation(user.getLocation());
        validateCompany(user.getCompany());
    }

    private void validateName(String firstname, String lastname, String username) {
        if (firstname.length() > 30) {
            throw new BadCredentialsException("Invalid firstname. Too many characters");
        }

        if (!firstname.matches("^[a-zA-Z]*$")) {
            throw new BadCredentialsException("Invalid firstname. Name should contain only characters");
        }

        if (lastname.length() > 30) {
            throw new BadCredentialsException("Invalid lastname. Too many characters");
        }

        if (!lastname.matches("^[a-zA-Z]*$")) {
            throw new BadCredentialsException("Invalid lastname. Name should contain only characters");
        }

        if (username.length() > 30) {
            throw new BadCredentialsException("Invalid username. Too many characters");
        }
    }

    private void validateEmail(String email) {
        if (email.length() > 50) {
            throw new BadCredentialsException("Invalid email. Too many characters");
        }

        if (!email.contains("@")) {
            throw new BadCredentialsException("Invalid email");
        }
    }

    private void validatePassword(String password) {
        PasswordValidator validator = new PasswordValidator(
                new LengthRule(8, 128),
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

    private void validateLocation(String location) {
        if(location.length() > 50) {
            throw new BadCredentialsException("Invalid location. Too many characters");
        }
    }

    private void validateCompany(String company) {
        if(company.length() > 50) {
            throw new BadCredentialsException("Invalid company. Too many characters");
        }
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request.firstname() == null || request.firstname().isEmpty()) {
            throw new BadCredentialsException("The First Name field is required.");
        }

        if (request.lastname() == null || request.lastname().isEmpty()) {
            throw new BadCredentialsException("The Last Name field is required.");
        }

        if (request.username() == null || request.username().isEmpty()) {
            throw new BadCredentialsException("The Username field is required.");
        }

        if (request.email() == null || request.email().isEmpty()) {
            throw new BadCredentialsException("The Email field is required.");
        }

        if (request.password() == null || request.password().isEmpty()) {
            throw new BadCredentialsException("The Password field is required.");
        }
    }

    private void validateAuthRequest(AuthRequest request) {
        if (request.email() == null || request.email().isEmpty()
                || request.password() == null || request.password().isEmpty()) {
            throw new BadCredentialsException("All fields are necessary");
        }
    }
}
