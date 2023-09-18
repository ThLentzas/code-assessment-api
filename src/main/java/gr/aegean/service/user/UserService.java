package gr.aegean.service.user;

import gr.aegean.entity.Analysis;
import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.entity.EmailUpdateToken;
import gr.aegean.entity.User;
import gr.aegean.mapper.dto.UserDTOMapper;
import gr.aegean.model.dto.analysis.AnalysisResult;
import gr.aegean.model.dto.user.UserAccountDeleteRequest;
import gr.aegean.model.dto.user.UserDTO;
import gr.aegean.model.dto.user.UserEmailUpdateRequest;
import gr.aegean.model.dto.user.UserHistory;
import gr.aegean.model.dto.user.UserPasswordUpdateRequest;
import gr.aegean.model.dto.user.UserProfile;
import gr.aegean.model.dto.user.UserProfileUpdateRequest;
import gr.aegean.repository.EmailUpdateRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.service.analysis.AnalysisService;
import gr.aegean.service.auth.JwtService;
import gr.aegean.service.email.EmailService;
import gr.aegean.utility.PasswordValidator;
import gr.aegean.utility.StringUtils;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UserService {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final EmailUpdateRepository emailUpdateRepository;
    private final EmailService emailService;
    private final AnalysisService analysisService;
    private final PasswordEncoder passwordEncoder;
    private static final String USER_NOT_FOUND_ERROR_MSG = "User not found with id: ";
    private final UserDTOMapper userDTOMapper = new UserDTOMapper();

    public void registerUser(User user) {
        if (userRepository.existsUserWithEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
        }

        if (userRepository.existsUserWithUsername(user.getUsername())) {
            throw new DuplicateResourceException("The provided username already exists");
        }

        userRepository.registerUser(user);
    }

    public UserProfile getProfile() {
        int userId = Integer.parseInt(jwtService.getSubject());

        return userRepository.findUserByUserId(userId)
                .map(user -> new UserProfile(
                        user.getFirstname(),
                        user.getLastname(),
                        user.getUsername(),
                        user.getBio(),
                        user.getLocation(),
                        user.getCompany()))
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_ERROR_MSG + userId));
    }

    public UserDTO findUser() {
        int userId = Integer.parseInt(jwtService.getSubject());
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                () -> new ResourceNotFoundException(USER_NOT_FOUND_ERROR_MSG + userId));

        return userDTOMapper.apply(user);
    }

    public void updateProfile(UserProfileUpdateRequest profileUpdateRequest) {
        int userId = Integer.parseInt(jwtService.getSubject());

        userRepository.findUserByUserId(userId)
                .ifPresentOrElse(user -> updateProfileProperties(user, profileUpdateRequest),
                        () -> {
                            throw new ResourceNotFoundException(USER_NOT_FOUND_ERROR_MSG + userId);
                        });
    }

    public void createEmailUpdateToken(UserEmailUpdateRequest emailUpdateRequest) {
        int userId = Integer.parseInt(jwtService.getSubject());

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

                    /*
                        If the user requested a new email update without clicking the link on the previous email, we
                        have to invalidate the previous generated tokens and save the latest one.
                     */
                    emailUpdateRepository.deleteAllUserTokens(user.getId());
                    emailUpdateRepository.saveToken(emailUpdateToken);

                    emailService.sendEmailVerification(emailUpdateRequest.email(), user.getUsername(), token);
                }, () -> {
                    throw new ResourceNotFoundException(USER_NOT_FOUND_ERROR_MSG + userId);
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
                                "Please request a new one");
                    }

                    /*
                        Update user's email and delete the relevant email token.
                     */
                    userRepository.updateEmail(emailUpdateToken.userId(), emailUpdateToken.email());
                    emailUpdateRepository.deleteToken(hashedToken);
                }, () -> {
                    throw new BadCredentialsException("Email update token is invalid");
                });
    }

    public void updatePassword(UserPasswordUpdateRequest passwordUpdateRequest) {
        int userId = Integer.parseInt(jwtService.getSubject());

        userRepository.findUserByUserId(userId)
                .ifPresentOrElse(user -> {
                    if (!passwordEncoder.matches(passwordUpdateRequest.oldPassword(), user.getPassword())) {
                        throw new BadCredentialsException("Old password is incorrect");
                    }

                    PasswordValidator.validatePassword(passwordUpdateRequest.newPassword());
                    userRepository.updatePassword(
                            userId,
                            passwordEncoder.encode(passwordUpdateRequest.newPassword()));
                }, () -> {
                    throw new ResourceNotFoundException(USER_NOT_FOUND_ERROR_MSG + userId);
                });
    }

    public UserHistory getHistory(String from, String to) {
        int userId = Integer.parseInt(jwtService.getSubject());

        List<AnalysisResult> history = new ArrayList<>();
        List<Analysis> analyses = null;
        AnalysisResult analysisResult;

        /*
            One of the two dates is null or empty.
         */
        if (((from == null || from.isBlank()) && to != null) || (from != null && (to == null || to.isBlank()))) {
            throw new IllegalArgumentException("Both from and to dates must be provided");
        }

        if (from != null && !from.isBlank() && !to.isBlank()) {
            StringUtils.validateDate(from);
            StringUtils.validateDate(to);

            Date fromDate = Date.valueOf(from);
            Date toDate = Date.valueOf(to);

            analyses = analysisService.getHistoryInDateRange(userId, fromDate, toDate);
        }

        if (analyses == null) {
            analyses = analysisService.getHistory(userId);
        }

        /*
            If the user has no previous history, we won't return 404, but 200 with an empty list.
         */
        if (analyses.isEmpty()) {
            return new UserHistory(history);
        }

        for (Analysis analysis : analyses) {
            analysisResult = analysisService.findAnalysisResultByAnalysisId(analysis.getId());
            history.add(analysisResult);
        }

        return new UserHistory(history);
    }

    public void deleteAccount(UserAccountDeleteRequest accountDeleteRequest) {
        int userId = Integer.parseInt(jwtService.getSubject());

        userRepository.findUserByUserId(userId)
                .ifPresentOrElse(user -> {
                    if (!passwordEncoder.matches(accountDeleteRequest.password(), user.getPassword())) {
                        throw new BadCredentialsException("Password is incorrect");
                    }
                    userRepository.deleteAccount(userId);
                    }, () -> {
                    throw new ResourceNotFoundException(USER_NOT_FOUND_ERROR_MSG + userId);
                });
    }

    public void validateUser(User user) {
        validateFirstname(user.getFirstname());
        validateLastname(user.getLastname());
        validateUsername(user.getUsername());
        validateEmail(user.getEmail());
        PasswordValidator.validatePassword(user.getPassword());
    }

    private void validateFirstname(String firstname) {
        if (firstname.length() > 30) {
            throw new IllegalArgumentException("Invalid firstname. Firstname must not exceed 30 characters");
        }

        if (!firstname.matches("^[a-zA-Z]*$")) {
            throw new IllegalArgumentException("Invalid firstname. Firstname should contain only characters");
        }
    }

    private void validateLastname(String lastname) {
        if (lastname.length() > 30) {
            throw new IllegalArgumentException("Invalid lastname. Lastname must not exceed 30 characters");
        }

        if (!lastname.matches("^[a-zA-Z]*$")) {
            throw new IllegalArgumentException("Invalid lastname. Lastname should contain only characters");
        }
    }

    private void validateUsername(String username) {
        if (username.length() > 30) {
            throw new IllegalArgumentException("Invalid username. Username must not exceed 30 characters");
        }
    }

    private void validateEmail(String email) {
        if (email.length() > 50) {
            throw new IllegalArgumentException("Invalid email. Email must not exceed 50 characters");
        }

        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    public void validateBio(String bio) {
        if (bio.length() > 150) {
            throw new IllegalArgumentException("Invalid bio. Bio must not exceed 150 characters");
        }
    }

    public void validateLocation(String location) {
        if (location.length() > 50) {
            throw new IllegalArgumentException("Invalid location. Location must not exceed 50 characters");
        }
    }

    public void validateCompany(String company) {
        if (company.length() > 50) {
            throw new IllegalArgumentException("Invalid company. Company must not exceed 50 characters");
        }
    }

    private void updateProfileProperties(User user, UserProfileUpdateRequest profileUpdateRequest) {
        updatePropertyIfNonNull(
                profileUpdateRequest.firstname(),
                this::validateFirstname,
                value -> userRepository.updateFirstname(user.getId(), value));

        updatePropertyIfNonNull(
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

        updatePropertyIfNonNull(
                profileUpdateRequest.bio(),
                this::validateBio,
                value -> userRepository.updateBio(user.getId(), value));

        updatePropertyIfNonNull(
                profileUpdateRequest.location(),
                this::validateLocation,
                value -> userRepository.updateLocation(user.getId(), value));

        updatePropertyIfNonNull(
                profileUpdateRequest.company(),
                this::validateCompany,
                value -> userRepository.updateCompany(user.getId(), value));
    }

    private void updatePropertyIfNonNull(String property, Consumer<String> validator, Consumer<String> updater) {
        if (property != null) {
            validator.accept(property);
            updater.accept(property);
        }
    }
}