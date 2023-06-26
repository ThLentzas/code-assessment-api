package gr.aegean.controller;

import gr.aegean.model.user.UserPasswordUpdateRequest;
import gr.aegean.model.user.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gr.aegean.model.user.UserEmailUpdateRequest;
import gr.aegean.model.user.UserProfileUpdateRequest;
import gr.aegean.service.UserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

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

    @PutMapping("/{userId}/settings/email")
    public ResponseEntity<Void> updateEmail(@PathVariable("userId") Integer userId,
                                            @Valid @RequestBody UserEmailUpdateRequest emailUpdateRequest) {


        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{userId}/settings/password")
    public ResponseEntity<Void> updatePassword(@PathVariable("userId") Integer userId,
                                               @Valid @RequestBody UserPasswordUpdateRequest passwordUpdateRequest) {
        userService.updatePassword(userId, passwordUpdateRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
