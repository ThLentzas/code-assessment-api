package gr.aegean.service.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import gr.aegean.model.UserPrincipal;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder jwtEncoder;

    /*
        The claims of the JwtToken are: issuer, when it is issued at, when it expires at, subject(user's id). Using the
        UserPrincipal allows to scale in case we need a custom claim for the user roles as we have access to the
        authorities
     */
    public String assignToken(UserPrincipal userPrincipal) {
        Instant now = Instant.now();
        long expiresIn = 2;

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(expiresIn, ChronoUnit.HOURS))
                .subject(userPrincipal.user().getId().toString())
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
