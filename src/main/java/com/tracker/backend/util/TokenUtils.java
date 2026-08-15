package com.tracker.backend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TokenUtils {
    public static String hashToken(String rawToken) {
        try {
            // Initialize MessageDigest with SHA-256 algorithm
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Perform the hashing process
            byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            // Convert byte array into a human-readable Hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing the token: Algorithm not found", e);
        }
    }
}
