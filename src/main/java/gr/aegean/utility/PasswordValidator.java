package gr.aegean.utility;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.RuleResult;


public final class PasswordValidator {

    private PasswordValidator() {

        // prevent instantiation
        throw new UnsupportedOperationException("PasswordValidator is a utility class and cannot be instantiated.");
    }

    /*
        Avoiding regex because it's error-prone, and you can miss edge cases.
     */
    public static void validatePassword(String password) {
        org.passay.PasswordValidator validator = new org.passay.PasswordValidator(
                new LengthRule(12, 128),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1)
        );

        RuleResult result = validator.validate(new PasswordData(password));
        if (!result.isValid()) {
            throw new IllegalArgumentException(validator.getMessages(result).get(0));
        }
    }
}
