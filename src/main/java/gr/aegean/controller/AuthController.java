package gr.aegean.controller;

import gr.aegean.security.auth.AuthRequest;
import gr.aegean.security.password.PasswordResetConfirmationRequest;
import gr.aegean.security.password.PasswordResetRequest;
import gr.aegean.security.password.PasswordResetResult;
import gr.aegean.service.PasswordResetService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import gr.aegean.security.auth.AuthResponse;
import gr.aegean.security.auth.RegisterRequest;
import gr.aegean.service.AuthService;

import jakarta.websocket.server.PathParam;

import lombok.RequiredArgsConstructor;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

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

    @PostMapping("/password_reset")
    public ResponseEntity<PasswordResetResult> resetPassword(@RequestBody PasswordResetRequest passwordResetRequest) {
        PasswordResetResult passwordResetResult = passwordResetService.createPasswordResetRequest(passwordResetRequest);

        return new ResponseEntity<>(passwordResetResult, HttpStatus.NO_CONTENT);
    }

    @GetMapping("/password_reset")
    public ResponseEntity<Void> resetPassword(@PathParam("token") String token) {
        passwordResetService.validatePasswordResetToken(token);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/password_reset/confirm")
    public ResponseEntity<Void> resetPassword(@RequestBody PasswordResetConfirmationRequest resetConfirmationRequest) {
        passwordResetService.resetPassword(resetConfirmationRequest);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}