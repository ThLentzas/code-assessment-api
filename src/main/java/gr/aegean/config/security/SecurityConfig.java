package gr.aegean.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;


@Configuration
@EnableWebSecurity(debug = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthenticationProvider authenticationProvider;

    /*
        Our permitAll() endpoints are user login/signup and password reset. We have to allow the GET request that will
        be sent when the user wants to update their email and click on the link we have sent in their new email. Same
        logic applies for the password reset email link, but it's covered from the 1st case.

        Password reset: "/api/v1/auth/password_reset?token="
        Email update: "/api/v1/user/email?token="
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll();
                    auth.requestMatchers(HttpMethod.PUT, "/api/v1/auth/password_reset/confirm").permitAll();
                    /*
                        In "/api/v1/user/email/**" => ** represents zero or more directories. In the case of the request
                        "/api/v1/user/email?token=token" we have 0 subdirectories, so it works. It doesn't mean anything
                        after '/' in that case "/api/v1/user/email?token=token" would fail cause there is no '/' after
                        email
                     */
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/user/email/**").permitAll();
                    auth.anyRequest().authenticated();
                })
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .build();
    }
}