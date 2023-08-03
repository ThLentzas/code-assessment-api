package gr.aegean.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gr.aegean.model.analysis.AnalysisResult;
import gr.aegean.model.user.UserUpdatePasswordRequest;
import gr.aegean.model.user.UserProfile;
import gr.aegean.model.user.UserUpdateEmailRequest;
import gr.aegean.model.user.UserProfileUpdateRequest;
import gr.aegean.service.user.UserService;

import jakarta.websocket.server.PathParam;
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
    public ResponseEntity<List<AnalysisResult>> getHistory(@PathVariable Integer userId,
                                                           @PathParam("from") String from,
                                                           @PathParam("to") String to) {
        List<AnalysisResult> history = userService.getHistory(userId, from, to);

        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    @DeleteMapping("/{userId}/history/{analysisId}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable Integer analysisId, @PathVariable Integer userId) {
        userService.deleteAnalysis(analysisId, userId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{userId}/settings/account")
    public ResponseEntity<Void> deleteAccount(@PathVariable Integer userId) {
        userService.deleteAccount(userId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
