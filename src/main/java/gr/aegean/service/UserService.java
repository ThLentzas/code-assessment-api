package gr.aegean.service;

import gr.aegean.exception.BadCredentialsException;
import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.model.user.User;
import gr.aegean.model.user.UserGeneralUpdateRequest;
import gr.aegean.repository.UserRepository;
import gr.aegean.utility.PasswordValidation;

import lombok.RequiredArgsConstructor;

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

    public void updateUser(Integer userId, UserGeneralUpdateRequest updateRequest) {
        if(updateRequest.firstname() != null && !updateRequest.firstname().isBlank()) {
            validateFirstname(updateRequest.firstname());

            userRepository.updateFirstname(userId, updateRequest.firstname());
        }

        if(updateRequest.lastname() != null && !updateRequest.lastname().isBlank()) {
            validateLastname(updateRequest.lastname());

            userRepository.updateLastname(userId, updateRequest.lastname());
        }

        if(updateRequest.username() != null && !updateRequest.username().isBlank()) {
            validateUsername(updateRequest.username());
            if(userRepository.existsUserWithUsername(updateRequest.username())) {
                throw new DuplicateResourceException("The provided username already exists");
            }

            userRepository.updateUsername(userId, updateRequest.username());
        }

        if(updateRequest.bio() != null && !updateRequest.bio().isBlank()) {
            validateBio(updateRequest.bio());

            userRepository.updateBio(userId, updateRequest.bio());
        }

        if(updateRequest.location() != null && !updateRequest.location().isBlank()) {
            validateLocation(updateRequest.location());

            userRepository.updateLocation(userId, updateRequest.location());
        }

        if(updateRequest.company() != null && !updateRequest.company().isBlank()) {
            validateCompany(updateRequest.company());

            userRepository.updateCompany(userId, updateRequest.company());
        }
    }

    public void validateUser(User user) {
        validateFirstname(user.getFirstname());
        validateLastname(user.getLastname());
        validateUsername(user.getUsername());
        validateEmail(user.getEmail());
        PasswordValidation.validatePassword(user.getPassword());
        validateBio(user.getBio());
        validateLocation(user.getLocation());
        validateCompany(user.getCompany());
    }

    private void validateFirstname(String firstname) {
        if (firstname.length() > 30) {
            throw new BadCredentialsException("Invalid firstname. Too many characters");
        }

        if (!firstname.matches("^[a-zA-Z]*$")) {
            throw new BadCredentialsException("Invalid firstname. Name should contain only characters");
        }
    }

    private void validateLastname(String lastname) {
        if (lastname.length() > 30) {
            throw new BadCredentialsException("Invalid lastname. Too many characters");
        }

        if (!lastname.matches("^[a-zA-Z]*$")) {
            throw new BadCredentialsException("Invalid lastname. Name should contain only characters");
        }
    }

    private void validateUsername(String username) {
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