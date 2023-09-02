package gr.aegean.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gr.aegean.service.user.UserService;
import gr.aegean.model.dto.user.UserHistory;
import gr.aegean.model.dto.user.UserProfile;
import gr.aegean.model.dto.user.UserProfileUpdateRequest;
import gr.aegean.model.dto.user.UserUpdateEmailRequest;
import gr.aegean.model.dto.user.UserUpdatePasswordRequest;

import jakarta.websocket.server.PathParam;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;


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
    public ResponseEntity<UserHistory> getHistory(@PathParam("from") String from,
                                                             @PathParam("to") String to,
                                                             HttpServletRequest request) {
        UserHistory history = userService.getHistory(request, from, to);

        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    @DeleteMapping("history/analysis/{analysisId}")
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
