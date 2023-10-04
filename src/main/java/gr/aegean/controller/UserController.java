package gr.aegean.controller;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gr.aegean.model.dto.user.UserAccountDeleteRequest;
import gr.aegean.model.dto.user.UserDTO;
import gr.aegean.model.dto.user.UserEmailUpdateRequest;
import gr.aegean.model.dto.user.UserHistory;
import gr.aegean.model.dto.user.UserPasswordUpdateRequest;
import gr.aegean.model.dto.user.UserProfile;
import gr.aegean.model.dto.user.UserProfileUpdateRequest;
import gr.aegean.service.user.UserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;


/*
    We can't use @RestController at a class level because it's a combination of @Controller + @ResponseBody which will
    send a json response, but we are expecting view resolver to redirect url, specifically after the user clicks the
    email update verification link.
 */
@Controller
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    @ResponseBody
    public ResponseEntity<UserDTO> getUser() {
        UserDTO userDTO = userService.findUser();

        return new ResponseEntity<>(userDTO, HttpStatus.OK);
    }

    @GetMapping("/profile")
    @ResponseBody
    public ResponseEntity<UserProfile> getProfile() {
        UserProfile profile = userService.getProfile();

        return new ResponseEntity<>(profile, HttpStatus.OK);
    }

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(@RequestBody UserProfileUpdateRequest profileUpdateRequest) {
        userService.updateProfile(profileUpdateRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/settings/email")
    public ResponseEntity<Void> updateEmail(@Valid @RequestBody UserEmailUpdateRequest emailUpdateRequest) {
        userService.createEmailUpdateToken(emailUpdateRequest);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    /*
        If the validation of the token in the verification link was successful the user is redirected to their profile
        and the email was updated successfully, otherwise the token was not valid and the user is redirected to an
        error page where they can request a new email update.
     */
    @GetMapping("/email")
    public String updateEmail(@RequestParam("token") @DefaultValue("") String token) {
        boolean updated = userService.updateEmail(token);
        if (updated) {
            return "redirect:http://localhost:4200/profile";
        }

        return "redirect:http://localhost:4200/email_update_error";
    }

    @PutMapping("/settings/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UserPasswordUpdateRequest passwordUpdateRequest) {
        userService.updatePassword(passwordUpdateRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/history")
    @ResponseBody
    public ResponseEntity<UserHistory> getHistory(@RequestParam("from") String from, @RequestParam("to") String to) {
        UserHistory history = userService.getHistory(from, to);

        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    /*
        DELETE request is allowed to have a body, but it is not recommended.
     */
    @PutMapping("/settings/account")
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody UserAccountDeleteRequest accountDeleteRequest) {
        userService.deleteAccount(accountDeleteRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
