package gr.aegean.service.auth;

import gr.aegean.entity.User;
import gr.aegean.mapper.dto.UserDTOMapper;
import gr.aegean.model.user.UserDTO;
import gr.aegean.model.auth.AuthResponse;
import gr.aegean.model.auth.RegisterRequest;
import gr.aegean.exception.UnauthorizedException;
import gr.aegean.model.auth.LoginRequest;
import gr.aegean.service.user.UserService;

import org.springframework.security.authentication.AuthenticationManager;
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
                .bio(request.bio())
                .location(request.location())
                .company(request.company())
                .build();

        userService.validateUser(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Integer userId = userService.registerUser(user);
        UserDTO userDTO = userDTOMapper.apply(user);
        String jwtToken = jwtService.assignToken(userDTO);

        return new AuthResponse(userId, jwtToken);
    }

    public AuthResponse loginUser(LoginRequest request) {
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

        return new AuthResponse(principal.getId(), jwtToken);
    }
}
