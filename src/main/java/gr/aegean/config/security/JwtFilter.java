package gr.aegean.config.security;

import gr.aegean.exception.UnauthorizedException;
import gr.aegean.service.auth.CookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import gr.aegean.entity.User;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.repository.UserRepository;
import gr.aegean.service.auth.JwtService;

import java.io.IOException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final CookieService cookieService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        //We allow the permit all endpoints
        if (request.getServletPath().contains("/api/v1/auth")) {
            filterChain.doFilter(request, response);

            return;
        }

        String token;
        try {
            token = cookieService.getTokenFromCookie(request);

        /*
            Exception Handlers don't work with Filters.
        */
        } catch (UnauthorizedException ue) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        String subject = jwtService.getSubject(token);

        if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = userRepository.findUserByUserId(Integer.parseInt(subject)).orElseThrow(() ->
                    new ServerErrorException("The server encountered an internal error and was unable to " +
                            "complete your request. Please try again later."));

            if (jwtService.isTokenValid(token, user)) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
