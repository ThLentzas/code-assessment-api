package gr.aegean.controller;

import gr.aegean.model.analysis.AnalysisResult;
import gr.aegean.model.user.UserUpdatePasswordRequest;
import gr.aegean.model.user.UserProfile;
import jakarta.websocket.server.PathParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gr.aegean.model.user.UserUpdateEmailRequest;
import gr.aegean.model.user.UserProfileUpdateRequest;
import gr.aegean.service.user.UserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import java.util.List;


@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfile> getProfile(@PathVariable("userId") Integer userId) {
        UserProfile profile = userService.getProfile(userId);

        return new ResponseEntity<>(profile, HttpStatus.OK);
    }

    @PutMapping("/{userId}/settings/profile")
    public ResponseEntity<Void> updateProfile(@PathVariable("userId") Integer userId,
                                              @RequestBody UserProfileUpdateRequest profileUpdateRequest) {
        userService.updateProfile(userId, profileUpdateRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{userId}/settings/email")
    public ResponseEntity<Void> updateEmail(@PathVariable("userId") Integer userId,
                                            @Valid @RequestBody UserUpdateEmailRequest emailUpdateRequest) {
        userService.createEmailUpdateToken(userId, emailUpdateRequest);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    // TODO: 7/20/2023 We need to review this endpoint. It's the email link that the user clicks, so we cant add the
    //  token via postman.
    @GetMapping("/settings/email")
    public ResponseEntity<Void> updateEmail(@PathParam("token") String token) {
        userService.updateEmail(token);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/{userId}/settings/password")
    public ResponseEntity<Void> updatePassword(@PathVariable("userId") Integer userId,
                                               @Valid @RequestBody UserUpdatePasswordRequest passwordUpdateRequest) {
        userService.updatePassword(userId, passwordUpdateRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<AnalysisResult>> getHistory(@PathVariable Integer userId) {
        List<AnalysisResult> history = userService.getHistory(userId);

        return new ResponseEntity<>(history, HttpStatus.OK);
    }
}
