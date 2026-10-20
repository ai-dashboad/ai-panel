package com.aipanel.common.util;

import com.aipanel.common.constant.CommonConstants;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for generating and hashing API Keys
 */
public final class ApiKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private ApiKeyGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generate a new API Key with the format: sk-{random_string}
     *
     * @return Generated API Key
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(CommonConstants.API_KEY_PREFIX);
        byte[] randomBytes = new byte[CommonConstants.API_KEY_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);

        for (int i = 0; i < CommonConstants.API_KEY_LENGTH; i++) {
            int index = (randomBytes[i] & 0xFF) % ALPHABET.length();
            sb.append(ALPHABET.charAt(index));
        }

        return sb.toString();
    }

    /**
     * Hash an API Key using SHA-256
     *
     * @param apiKey The API Key to hash
     * @return SHA-256 hash of the API Key
     */
    public static String hash(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API Key cannot be null or empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(apiKey.getBytes());
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generate the display prefix for an API Key (e.g., sk-...abc)
     *
     * @param apiKey The full API Key
     * @return Display prefix
     */
    public static String getDisplayPrefix(String apiKey) {
        if (apiKey == null || apiKey.length() < 12) {
            return "***";
        }

        int prefixLen = CommonConstants.API_KEY_DISPLAY_PREFIX_LENGTH;
        int suffixLen = CommonConstants.API_KEY_DISPLAY_SUFFIX_LENGTH;

        return apiKey.substring(0, prefixLen) + "..." +
               apiKey.substring(apiKey.length() - suffixLen);
    }

    /**
     * Validate API Key format
     *
     * @param apiKey The API Key to validate
     * @return true if format is valid
     */
    public static boolean isValidFormat(String apiKey) {
        if (apiKey == null) {
            return false;
        }

        if (!apiKey.startsWith(CommonConstants.API_KEY_PREFIX)) {
            return false;
        }

        String keyPart = apiKey.substring(CommonConstants.API_KEY_PREFIX.length());
        if (keyPart.length() != CommonConstants.API_KEY_LENGTH) {
            return false;
        }

        for (char c : keyPart.toCharArray()) {
            if (ALPHABET.indexOf(c) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Extract key ID from API Key (first 8 chars after prefix)
     *
     * @param apiKey The API Key
     * @return Key ID
     */
    public static String extractKeyId(String apiKey) {
        if (apiKey == null || !apiKey.startsWith(CommonConstants.API_KEY_PREFIX)) {
            return null;
        }

        String keyPart = apiKey.substring(CommonConstants.API_KEY_PREFIX.length());
        return keyPart.length() >= 8 ? keyPart.substring(0, 8) : keyPart;
    }

    /**
     * Generate a unique request ID
     *
     * @return Request ID
     */
    public static String generateRequestId() {
        byte[] randomBytes = new byte[12];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "req_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Generate a unique response ID
     *
     * @return Response ID
     */
    public static String generateResponseId() {
        byte[] randomBytes = new byte[12];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "chatcmpl-" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }
}
