package gr.aegean.controller;

import gr.aegean.security.auth.AuthRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import gr.aegean.security.auth.AuthResponse;
import gr.aegean.security.auth.RegisterRequest;
import gr.aegean.service.AuthService;

import lombok.RequiredArgsConstructor;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    /**
     * @return a ResponseEntity containing the authentication token.
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        AuthResponse authResponse = authService.register(request);

        URI location = uriBuilder
                .path("/api/v1/users/{userID}")
                .buildAndExpand(authResponse.getId())
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(authResponse, headers, HttpStatus.CREATED);
    }

    /**
     * @return a ResponseEntity containing the authentication token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest request) {
        AuthResponse authResponse = authService.authenticate(request);

        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }
}