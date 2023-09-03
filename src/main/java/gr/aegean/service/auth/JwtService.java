package gr.aegean.service.auth;

import gr.aegean.entity.User;
import gr.aegean.exception.ServerErrorException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import gr.aegean.model.dto.user.UserDTO;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    /**
     * The claims of the JwtToken are: issuer, when it is issued at, when it expires at, subject(user's email).
     */
    public String assignToken(UserDTO userDTO) {
        Instant now = Instant.now();
        long expiresIn = 2;

        JwtClaimsSet claims = JwtClaimsSet
                .builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(expiresIn, ChronoUnit.HOURS))
                .subject(userDTO.id().toString())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String getSubjectFromJwt(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        String bearerToken = null;

        if (!token.isBlank() && token.startsWith("Bearer ")) {
            bearerToken = token.substring(7);
        }

        Jwt jwt = jwtDecoder.decode(bearerToken);

        return jwt.getSubject();
    }
}
