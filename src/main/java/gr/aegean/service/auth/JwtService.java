package gr.aegean.service.auth;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.Function;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import gr.aegean.model.dto.user.UserDTO;
import gr.aegean.entity.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${jwt.secretKey}")
    private String secretKey;

    public String assignToken(UserDTO userDTO) {
        Instant now = Instant.now();
        long expiresIn = 2;

        /*
            The subject of the token is the user's id.
         */
        return Jwts.builder()
                .setSubject(userDTO.id().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expiresIn, ChronoUnit.HOURS)))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, User user) {
        int userId = Integer.parseInt(getSubject(token));

        return (userId == user.getId()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return getExpiration(token).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);

        return claimsResolver.apply(claims);
    }

    private Date getExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String getSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
