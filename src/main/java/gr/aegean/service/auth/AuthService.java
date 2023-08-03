package gr.aegean.service.auth;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.entity.User;
import gr.aegean.mapper.dto.UserDTOMapper;
import gr.aegean.model.user.UserDTO;
import gr.aegean.model.auth.AuthResponse;
import gr.aegean.model.auth.RegisterRequest;
import gr.aegean.exception.UnauthorizedException;
import gr.aegean.model.auth.AuthRequest;
import gr.aegean.service.user.UserService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDTOMapper userDTOMapper;
    private final JwtDecoder jwtDecoder;

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
        user.setPassword(passwordEncoder.encode(request.password()));
        UserDTO userDTO = userDTOMapper.apply(user);

        Integer userId = userService.registerUser(user);
        String jwtToken = jwtService.assignToken(userDTO);

        return new AuthResponse(userId, jwtToken);
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

        return new AuthResponse(principal.getId(), jwtToken);
    }

    /*
        Extracting email from jwt, to call findUserByEmail() in order to get the userId.
    */
    public Integer getIdFromSubject(HttpServletRequest httpServletRequest) {
        String email = getSubjectFromJwt(httpServletRequest);

        return userService.findUserByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ServerErrorException("The server encountered an internal error and was unable " +
                        "to complete your request. Please try again later."));
    }

    private String getSubjectFromJwt(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        String bearerToken = null;

        if (!token.isBlank() && token.startsWith("Bearer ")) {
            bearerToken = token.substring(7);
        }

        Jwt jwt = jwtDecoder.decode(bearerToken);

        return jwt.getSubject();
    }
}
