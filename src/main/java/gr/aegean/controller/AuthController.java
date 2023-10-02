package gr.aegean.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gr.aegean.model.dto.auth.LoginRequest;
import gr.aegean.model.dto.auth.PasswordResetConfirmationRequest;
import gr.aegean.model.dto.auth.PasswordResetRequest;
import gr.aegean.model.dto.auth.AuthResponse;
import gr.aegean.model.dto.auth.RegisterRequest;
import gr.aegean.service.auth.AuthService;
import gr.aegean.service.auth.PasswordResetService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.registerUser(request);

        return new ResponseEntity<>(authResponse, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.loginUser(request);

        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }

    /*
        Best implementation for a password reset feature needs rate limiting to avoid username enumerations.
     */
    @PostMapping("/password_reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest passwordResetRequest) {
        passwordResetService.createPasswordResetToken(passwordResetRequest);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    /*
        If the validation of the token in the verification link was successful NO_CONTENT is returned and the password
        was reset successfully, otherwise the token was not valid so UNAUTHORIZED is returned to indicate that the user
        didn't have the necessary credentials(valid token).
     */
    @PutMapping("/password_reset/confirm")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody
                                              PasswordResetConfirmationRequest resetConfirmationRequest) {
        boolean reset = passwordResetService.resetPassword(resetConfirmationRequest);

        if (reset) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
}