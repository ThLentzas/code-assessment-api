package gr.aegean.service;

import gr.aegean.exception.BadCredentialsException;
import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.model.user.User;
import gr.aegean.model.user.UserUpdateRequest;
import gr.aegean.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.passay.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    /**
     * @return the ID of the newly registered user for the URI
     */
    public Integer registerUser(User user) {
        if(userRepository.existsUserWithEmail(user.getEmail())) {
            throw new DuplicateResourceException("The provided email already exists");
        }

        if(userRepository.existsUserWithUsername(user.getUsername())) {
            throw new DuplicateResourceException("The provided username already exists");
        }

        return userRepository.registerUser(user);
    }

    public void updateUser(UserUpdateRequest userUpdateRequest) {

    }

    public void validateUser(User user) {
        validateName(user.getFirstname(), user.getLastname(), user.getUsername());
        validateEmail(user.getEmail());
        validatePassword(user.getPassword());
        validateBio(user.getBio());
        validateLocation(user.getLocation());
        validateCompany(user.getCompany());
    }

    private void validateName(String firstname, String lastname, String username) {
        if (firstname.length() > 30) {
            throw new BadCredentialsException("Invalid firstname. Too many characters");
        }

        if (!firstname.matches("^[a-zA-Z]*$")) {
            throw new BadCredentialsException("Invalid firstname. Name should contain only characters");
        }

        if (lastname.length() > 30) {
            throw new BadCredentialsException("Invalid lastname. Too many characters");
        }

        if (!lastname.matches("^[a-zA-Z]*$")) {
            throw new BadCredentialsException("Invalid lastname. Name should contain only characters");
        }

        if (username.length() > 30) {
            throw new BadCredentialsException("Invalid username. Too many characters");
        }
    }



    private void validateEmail(String email) {
        if (email.length() > 50) {
            throw new BadCredentialsException("Invalid email. Too many characters");
        }

        if (!email.contains("@")) {
            throw new BadCredentialsException("Invalid email");
        }
    }

    public void validatePassword(String password) {
        PasswordValidator validator = new PasswordValidator(
                new LengthRule(8, 128),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1)
        );

        RuleResult result = validator.validate(new PasswordData(password));
        if (!result.isValid()) {
            throw new BadCredentialsException(validator.getMessages(result).get(0));
        }
    }

    public void validateBio(String bio) {
        if (bio.length() > 150) {
            throw new BadCredentialsException("Invalid bio. Too many characters");
        }
    }

    public void validateLocation(String location) {
        if (location.length() > 50) {
            throw new BadCredentialsException("Invalid location. Too many characters");
        }
    }

    public void validateCompany(String company) {
        if (company.length() > 50) {
            throw new BadCredentialsException("Invalid company. Too many characters");
        }
    }
}