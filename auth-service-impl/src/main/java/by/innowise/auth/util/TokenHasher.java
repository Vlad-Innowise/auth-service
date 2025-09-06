package by.innowise.auth.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@UtilityClass
public class TokenHasher {

    private static final String SHA_256_ALGORITHM = "SHA-256";

    public static String hashSha256(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            byte[] hashedToken = messageDigest.digest(token.getBytes());
            return HexFormat.of().formatHex(hashedToken);
        } catch (NoSuchAlgorithmException e) {
            log.error("{} hash algorithm is not available", SHA_256_ALGORITHM, e);
            throw new RuntimeException(
                    "Failed to hash token within %s algorithm. It's not supported!".formatted(SHA_256_ALGORITHM));
        }
    }

}
