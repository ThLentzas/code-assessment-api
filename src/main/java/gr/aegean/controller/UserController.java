package gr.aegean.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import gr.aegean.model.dto.user.UserAccountDeleteRequest;
import gr.aegean.model.dto.user.UserDTO;
import gr.aegean.model.dto.user.UserEmailUpdateRequest;
import gr.aegean.model.dto.user.UserHistory;
import gr.aegean.model.dto.user.UserPasswordUpdateRequest;
import gr.aegean.model.dto.user.UserProfile;
import gr.aegean.model.dto.user.UserProfileUpdateRequest;
import gr.aegean.service.user.UserService;

import jakarta.websocket.server.PathParam;
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

    @GetMapping("/email")
    public String updateEmail(@PathParam("token") String token) {
        userService.updateEmail(token);

        return "redirect:http://localhost:4200/profile";
    }

    @PutMapping("/settings/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UserPasswordUpdateRequest passwordUpdateRequest) {
        userService.updatePassword(passwordUpdateRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/history")
    @ResponseBody
    public ResponseEntity<UserHistory> getHistory(@PathParam("from") String from, @PathParam("to") String to) {
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
