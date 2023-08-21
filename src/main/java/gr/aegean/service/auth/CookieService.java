package gr.aegean.service.auth;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import gr.aegean.exception.UnauthorizedException;

import java.time.Duration;

@Service
public class CookieService {

    public ResponseCookie createHttpOnlyCookie(String token) {
        return ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(false)
                .maxAge(Duration.ofHours(1))
                .sameSite("Lax")
                .build();
    }

    public String getTokenFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, "accessToken");

        if (cookie == null || cookie.getValue().isBlank()) {
            throw new UnauthorizedException("Unauthorized");
        }

        return cookie.getValue();
    }
}
