package gr.aegean.service;

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
import gr.aegean.mapper.UserDTOMapper;
import gr.aegean.model.user.UserDTO;
import gr.aegean.security.auth.AuthResponse;
import gr.aegean.security.auth.RegisterRequest;
import gr.aegean.exception.BadCredentialsException;
import gr.aegean.exception.UnauthorizedException;
import gr.aegean.security.auth.AuthRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDTOMapper userDTOMapper;

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

        userService.validateUser(user);
        user.setPassword(passwordEncoder.encode(request.password()));
        UserDTO userDTO = userDTOMapper.apply(user);

        Integer id = userService.registerUser(user);
        String jwtToken = jwtService.assignToken(userDTO);

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

        User principal = (User) authentication.getPrincipal();
        UserDTO userDTO = userDTOMapper.apply(principal);
        String jwtToken = jwtService.assignToken(userDTO);

        return new AuthResponse(jwtToken);
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
