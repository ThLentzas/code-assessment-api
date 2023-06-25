package gr.aegean.service;

import gr.aegean.model.user.User;
import gr.aegean.mapper.UserDTOMapper;
import gr.aegean.model.user.UserDTO;
import gr.aegean.model.auth.AuthResponse;
import gr.aegean.model.auth.RegisterRequest;
import gr.aegean.exception.UnauthorizedException;
import gr.aegean.model.auth.AuthRequest;

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
    private final UserDTOMapper userDTOMapper;

    public AuthResponse registerUser(RegisterRequest request) {
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

    public AuthResponse authenticateUser(AuthRequest request) {
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
}
