package gr.aegean.controller;

import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(HttpServletRequest request) {
        UserProfile profile = userService.getProfile(request);

        return new ResponseEntity<>(profile, HttpStatus.OK);
    }

    @PutMapping("/settings/profile")
    public ResponseEntity<Void> updateProfile(@RequestBody UserProfileUpdateRequest profileUpdateRequest,
                                              HttpServletRequest request) {
        userService.updateProfile(request, profileUpdateRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/settings/email")
    public ResponseEntity<Void> updateEmail(@Valid @RequestBody UserUpdateEmailRequest emailUpdateRequest,
                                            HttpServletRequest request) {
        userService.createEmailUpdateToken(request, emailUpdateRequest);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @GetMapping("/settings/email")
    public ResponseEntity<Void> updateEmail(@PathParam("token") String token) {
        userService.updateEmail(token);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/settings/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UserUpdatePasswordRequest passwordUpdateRequest,
                                               HttpServletRequest request) {
        userService.updatePassword(request, passwordUpdateRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/history")
    public ResponseEntity<List<AnalysisResult>> getHistory(@PathParam("from") String from,
                                                           @PathParam("to") String to,
                                                           HttpServletRequest request) {
        List<AnalysisResult> history = userService.getHistory(request, from, to);

        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    @DeleteMapping("history/{analysisId}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable Integer analysisId,
                                               HttpServletRequest request) {
        userService.deleteAnalysis(analysisId, request);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/settings/account")
    public ResponseEntity<Void> deleteAccount(HttpServletRequest request) {
        userService.deleteAccount(request);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
