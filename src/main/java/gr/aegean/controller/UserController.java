package gr.aegean.controller;

import gr.aegean.model.dto.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import gr.aegean.service.user.UserService;

import jakarta.websocket.server.PathParam;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;


/*
    We can't use @RestController at a class level because it's a combination of @Controller + @ResponseBody which will
    send a json response, but we are expecting view resolver to redirect url, specifically after the user clicks the
    email update verification link.
 */
@Controller
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    @ResponseBody
    public ResponseEntity<UserDTO> getUser(HttpServletRequest request) {
        UserDTO userDTO = userService.findUser(request);

        return new ResponseEntity<>(userDTO, HttpStatus.OK);
    }

    @GetMapping("/profile")
    @ResponseBody
    public ResponseEntity<UserProfile> getProfile(HttpServletRequest request) {
        UserProfile profile = userService.getProfile(request);

        return new ResponseEntity<>(profile, HttpStatus.OK);
    }

    @PutMapping("/profile")
    @ResponseBody
    public ResponseEntity<Void> updateProfile(@RequestBody UserProfileUpdateRequest profileUpdateRequest,
                                              HttpServletRequest request) {
        userService.updateProfile(request, profileUpdateRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/settings/email")
    @ResponseBody
    public ResponseEntity<Void> updateEmail(@Valid @RequestBody UserEmailUpdateRequest emailUpdateRequest,
                                            HttpServletRequest request) {
        userService.createEmailUpdateToken(request, emailUpdateRequest);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @GetMapping("/email")
    public String updateEmail(@PathParam("token") String token) {
        userService.updateEmail(token);

        return "redirect:http://localhost:4200/profile";
    }

    @PutMapping("/settings/password")
    @ResponseBody
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UserPasswordUpdateRequest passwordUpdateRequest,
                                               HttpServletRequest request) {
        userService.updatePassword(request, passwordUpdateRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/history")
    @ResponseBody
    public ResponseEntity<UserHistory> getHistory(@PathParam("from") String from,
                                                  @PathParam("to") String to,
                                                  HttpServletRequest request) {
        UserHistory history = userService.getHistory(request, from, to);

        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    @DeleteMapping("/settings/account")
    @ResponseBody
    public ResponseEntity<Void> deleteAccount(HttpServletRequest request) {
        userService.deleteAccount(request);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
