package gr.aegean.service;

import gr.aegean.model.user.User;
import gr.aegean.repository.UserRepository;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public Integer registerUser(User user) {
        return null;
    }



}
