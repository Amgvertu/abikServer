package info.prorabka.abik.server.common.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Хеширование токенов (SHA-256).
 * Используется для refresh-токенов, чтобы в БД не хранить их в открытом виде.
 */
@Component
public class TokenHasher {

    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 есть в любой JVM — этот блок недостижим
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }
}
