package gr.aegean.utility;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;

import gr.aegean.exception.ServerErrorException;

import org.bouncycastle.util.encoders.Hex;


public final class StringUtils {

    private StringUtils() {

        // prevent instantiation
        throw new UnsupportedOperationException("StringUtils is a utility class and cannot be instantiated.");
    }

    public static String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[128];

        secureRandom.nextBytes(randomBytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    public static String hashToken(String token) {
        byte[] hash;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException sae) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }

        return new String(Hex.encode(hash));
    }

    public static void validateDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            LocalDate.parse(date, formatter);
        } catch (DateTimeParseException dte) {
            throw new IllegalArgumentException("The provided date is invalid");
        }
    }
}


