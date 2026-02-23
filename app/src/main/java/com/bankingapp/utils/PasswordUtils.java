package com.bankingapp.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtils {

    // Hash a password with SHA-256
    public static String hash(String password) {
        try {
            MessageDigest md    = MessageDigest.getInstance("SHA-256");
            byte[]        bytes = md.digest(password.getBytes());
            StringBuilder sb    = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback: return plain text (shouldn't happen on Android)
            return password;
        }
    }

    // Validate password input
    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters";
        }
        return null;  // null means valid
    }

    // Validate username input
    public static String validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "Username cannot be empty";
        }
        if (username.length() < 3) {
            return "Username must be at least 3 characters";
        }
        if (!username.matches("[a-zA-Z0-9_]+")) {
            return "Username can only contain letters, numbers, and underscore";
        }
        return null;  // null means valid
    }
}
