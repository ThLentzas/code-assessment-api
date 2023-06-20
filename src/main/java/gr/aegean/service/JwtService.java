package gr.aegean.service;

import gr.aegean.model.user.UserDTO;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder jwtEncoder;

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
                .subject(userDTO.email())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
