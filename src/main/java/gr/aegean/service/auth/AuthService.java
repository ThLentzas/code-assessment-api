package gr.aegean.service.auth;

import gr.aegean.entity.User;
import gr.aegean.mapper.dto.UserDTOMapper;
import gr.aegean.model.UserPrincipal;
import gr.aegean.model.dto.user.UserDTO;
import gr.aegean.model.dto.auth.AuthResponse;
import gr.aegean.model.dto.auth.RegisterRequest;
import gr.aegean.exception.UnauthorizedException;
import gr.aegean.model.dto.auth.LoginRequest;
import gr.aegean.service.user.UserService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDTOMapper userDTOMapper = new UserDTOMapper();

    public AuthResponse registerUser(RegisterRequest request) {
        User user = User.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .username(request.username())
                .email(request.email())
                .password(request.password())
                .build();

        userService.validateUser(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.registerUser(user);

        UserDTO userDTO = userDTOMapper.apply(user);
        String jwtToken = jwtService.assignToken(userDTO);

        return new AuthResponse(jwtToken);
    }

    public AuthResponse loginUser(LoginRequest request) {
        Authentication authentication;

        /*
            In either case, meaning if user is not found or the password is wrong Spring will throw a
            spring.security.BadCredentialsException.
         */
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException bce) {
            throw new UnauthorizedException("Username or password is incorrect");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UserDTO userDTO = userDTOMapper.apply(principal.getUser());
        String jwtToken = jwtService.assignToken(userDTO);

        return new AuthResponse(jwtToken);
    }
}
