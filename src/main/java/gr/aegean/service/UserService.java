package gr.aegean.service;

import gr.aegean.exception.BadCredentialsException;
import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.model.token.EmailUpdateToken;
import gr.aegean.model.user.*;
import gr.aegean.repository.EmailUpdateRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.utility.PasswordValidation;

import gr.aegean.utility.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EmailUpdateRepository emailUpdateRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * @return the ID of the newly registered user for the URI
     */
    public Integer registerUser(User user) {
        if (userRepository.existsUserWithEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
        }

        if (userRepository.existsUserWithUsername(user.getUsername())) {
            throw new DuplicateResourceException("The provided username already exists");
        }

        return userRepository.registerUser(user);
    }

    public UserProfile getProfile(Integer userId) {
        return userRepository.findUserByUserId(userId)
                .map(user -> new UserProfile(
                        user.getFirstname(),
                        user.getLastname(),
                        user.getUsername(),
                        user.getBio(),
                        user.getLocation(),
                        user.getCompany()))
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " not found"));
    }


    public void updateProfile(Integer userId, UserProfileUpdateRequest profileUpdateRequest) {
        userRepository.findUserByUserId(userId)
                .ifPresentOrElse(user -> {
                    if (profileUpdateRequest.firstname() != null && !profileUpdateRequest.firstname().isBlank()) {
                        validateFirstname(profileUpdateRequest.firstname());

                        userRepository.updateFirstname(userId, profileUpdateRequest.firstname());
                    }

                    if (profileUpdateRequest.lastname() != null && !profileUpdateRequest.lastname().isBlank()) {
                        validateLastname(profileUpdateRequest.lastname());

                        userRepository.updateLastname(userId, profileUpdateRequest.lastname());
                    }

                    if (profileUpdateRequest.username() != null && !profileUpdateRequest.username().isBlank()) {
                        validateUsername(profileUpdateRequest.username());
                        if (userRepository.existsUserWithUsername(profileUpdateRequest.username())) {
                            throw new DuplicateResourceException("The provided username already exists");
                        }

                        userRepository.updateUsername(userId, profileUpdateRequest.username());
                    }

                    if (profileUpdateRequest.bio() != null && !profileUpdateRequest.bio().isBlank()) {
                        validateBio(profileUpdateRequest.bio());

                        userRepository.updateBio(userId, profileUpdateRequest.bio());
                    }

                    if (profileUpdateRequest.location() != null && !profileUpdateRequest.location().isBlank()) {
                        validateLocation(profileUpdateRequest.location());

                        userRepository.updateLocation(userId, profileUpdateRequest.location());
                    }

                    if (profileUpdateRequest.company() != null && !profileUpdateRequest.company().isBlank()) {
                        validateCompany(profileUpdateRequest.company());

                        userRepository.updateCompany(userId, profileUpdateRequest.company());
                    }
                }, () -> {
                    throw new ResourceNotFoundException("User with id: " + userId + " not found");
                });
    }

    public void createEmailUpdateToken(Integer userId, UserEmailUpdateRequest emailUpdateRequest) {
        userRepository.findUserByUserId(userId)
                .ifPresentOrElse(user -> {
                    if (!passwordEncoder.matches(emailUpdateRequest.password(), user.getPassword())) {
                        throw new BadCredentialsException("Wrong password");
                    }

                    validateEmail(emailUpdateRequest.email());
                    if (userRepository.existsUserWithEmail(emailUpdateRequest.email())) {
                        throw new DuplicateResourceException("Email already in use");
                    }

                    String token = StringUtils.generateToken();
                    String hashedToken = StringUtils.hashToken(token);
                    LocalDateTime expiryDate = LocalDateTime.now().plusDays(1);
                    EmailUpdateToken emailUpdateToken = new EmailUpdateToken(
                            userId,
                            hashedToken,
                            emailUpdateRequest.email(),
                            expiryDate
                    );
                    emailUpdateRepository.createToken(emailUpdateToken);

                    emailService.sendEmailVerification(emailUpdateRequest.email(), user.getUsername(), token);
                }, () -> {
                    throw new ResourceNotFoundException("User with id: " + userId + " not found");
                });
    }

    public void updateEmail(String token) {
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException("Email update token is invalid");
        }

        String hashedToken = StringUtils.hashToken(token);

        emailUpdateRepository.findToken(hashedToken)
                .ifPresentOrElse(emailUpdateToken -> {
                    if (emailUpdateToken.expiryDate().isBefore(LocalDateTime.now())) {
                        throw new BadCredentialsException("The email verification link has expired. " +
                                "Please request a new one.");
                    }

                    userRepository.updateEmail(emailUpdateToken.userId(), emailUpdateToken.email());
                    emailUpdateRepository.deleteToken(hashedToken);
                }, () -> {
                    throw new BadCredentialsException("Email update token is invalid");
                });
    }

    public void updatePassword(Integer userId, UserPasswordUpdateRequest passwordUpdateRequest) {
        userRepository.findUserByUserId(userId)
                .ifPresentOrElse(user -> {
                    if (!passwordEncoder.matches(passwordUpdateRequest.oldPassword(), user.getPassword())) {
                        throw new BadCredentialsException("Old password is incorrect");
                    }

                    PasswordValidation.validatePassword(passwordUpdateRequest.updatedPassword());
                    userRepository.updatePassword(
                            userId,
                            passwordEncoder.encode(passwordUpdateRequest.updatedPassword()));
                }, () -> {
                    throw new ResourceNotFoundException("User with id: " + userId + " not found");
                });
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