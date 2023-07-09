package gr.aegean.service;

import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.model.token.EmailUpdateToken;
import gr.aegean.model.user.User;
import gr.aegean.model.user.UserEmailUpdateRequest;
import gr.aegean.model.user.UserPasswordUpdateRequest;
import gr.aegean.model.user.UserProfile;
import gr.aegean.model.user.UserProfileUpdateRequest;
import gr.aegean.repository.EmailUpdateRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.utility.StringUtils;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;


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
                .ifPresentOrElse(user -> updateProfileProperties(user, profileUpdateRequest),
                        () -> { throw new ResourceNotFoundException("User with id: " + userId + " not found"); });
    }

    private void updateProfileProperties(User user, UserProfileUpdateRequest profileUpdateRequest) {
        updatePropertyIfNonNullAndNotBlank(
                profileUpdateRequest.firstname(),
                this::validateFirstname,
                value -> userRepository.updateFirstname(user.getId(), value));

        updatePropertyIfNonNullAndNotBlank(
                profileUpdateRequest.lastname(),
                this::validateLastname,
                value -> userRepository.updateLastname(user.getId(), value));

        if (profileUpdateRequest.username() != null && !profileUpdateRequest.username().isBlank()) {
            validateUsername(profileUpdateRequest.username());
            if (userRepository.existsUserWithUsername(profileUpdateRequest.username())) {
                throw new DuplicateResourceException("The provided username already exists");
            }

            userRepository.updateUsername(user.getId(), profileUpdateRequest.username());
        }

        updatePropertyIfNonNullAndNotBlank(
                profileUpdateRequest.bio(),
                this::validateBio,
                value -> userRepository.updateBio(user.getId(), value));

        updatePropertyIfNonNullAndNotBlank(
                profileUpdateRequest.location(),
                this::validateLocation,
                value -> userRepository.updateLocation(user.getId(), value));

        updatePropertyIfNonNullAndNotBlank(
                profileUpdateRequest.company(),
                this::validateCompany,
                value -> userRepository.updateCompany(user.getId(), value));
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

                    StringUtils.validatePassword(passwordUpdateRequest.updatedPassword());
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
        StringUtils.validatePassword(user.getPassword());
        validateBio(user.getBio());
        validateLocation(user.getLocation());
        validateCompany(user.getCompany());
    }

    private void validateFirstname(String firstname) {
        if (firstname.length() > 30) {
            throw new IllegalArgumentException("Invalid firstname. Too many characters");
        }

        if (!firstname.matches("^[a-zA-Z]*$")) {
            throw new IllegalArgumentException("Invalid firstname. Name should contain only characters");
        }
    }

    private void validateLastname(String lastname) {
        if (lastname.length() > 30) {
            throw new IllegalArgumentException("Invalid lastname. Too many characters");
        }

        if (!lastname.matches("^[a-zA-Z]*$")) {
            throw new IllegalArgumentException("Invalid lastname. Name should contain only characters");
        }
    }

    private void validateUsername(String username) {
        if (username.length() > 30) {
            throw new IllegalArgumentException("Invalid username. Too many characters");
        }
    }

    private void validateEmail(String email) {
        if (email.length() > 50) {
            throw new IllegalArgumentException("Invalid email. Too many characters");
        }

        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }

    public void validateBio(String bio) {
        if (bio.length() > 150) {
            throw new IllegalArgumentException("Invalid bio. Too many characters");
        }
    }

    public void validateLocation(String location) {
        if (location.length() > 50) {
            throw new IllegalArgumentException("Invalid location. Too many characters");
        }
    }

    public void validateCompany(String company) {
        if (company.length() > 50) {
            throw new IllegalArgumentException("Invalid company. Too many characters");
        }
    }

    private void updatePropertyIfNonNullAndNotBlank(String property,
                                                    Consumer<String> validator,
                                                    Consumer<String> updater) {
        if (property != null && !property.isBlank()) {
            validator.accept(property);
            updater.accept(property);
        }
    }
}