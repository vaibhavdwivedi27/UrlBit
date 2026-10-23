package in.urlbit.links.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Generates 7-character cryptographically random Base62 codes.
 *
 * <p>Uses SecureRandom to select from [0-9A-Za-z] (62^7 = 3.52 trillion combinations). Sequential
 * IDs are deliberately rejected to prevent link enumeration and guessing attacks.
 */
@Component
public class Base62CodeGenerator {

    public static final int CODE_LENGTH = 7;
    private static final String BASE62_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        char[] result = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(BASE62_CHARS.length());
            result[i] = BASE62_CHARS.charAt(index);
        }
        return new String(result);
    }
}
