package gr.aegean.service.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import gr.aegean.model.dto.user.UserDTO;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder jwtEncoder;

    /*
        The claims of the JwtToken are: issuer, when it is issued at, when it expires at, subject(user's id).
     */
    public String assignToken(UserDTO userDTO) {
        Instant now = Instant.now();
        long expiresIn = 2;

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(expiresIn, ChronoUnit.HOURS))
                .subject(userDTO.id().toString())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /*
        https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html

        The resulting Authentication#getPrincipal, by default, is a Spring Security Jwt object, and
        Authentication#getName maps to the JWT’s sub property, if one is present.

        Alternative approach:
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        jwt.getSubject();
     */
    public String getSubject() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
