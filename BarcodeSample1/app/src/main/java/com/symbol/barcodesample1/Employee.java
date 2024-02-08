package com.symbol.barcodesample1;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Employee {
    private static final Random random = new SecureRandom();
    private static final String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int iterations = 10000;
    private static final int keylength = 255;

    private final String number;
    private final String name;
    private final int level;
    private String password;

    public Employee(String number, String name, int level) {
        this.number = number;
        this.name = name;
        this.level = level;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public static String getSaltValue(int length) {
        StringBuilder finalVal = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            finalVal.append(characters.charAt(random.nextInt(characters.length())));
        }

        return new String(finalVal);
    }

    public static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keylength);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    public static String generateSecurePassword(String password, String salt)
    {
        byte[] securePassword = hash(password.toCharArray(), salt.getBytes());

        return new String(securePassword, StandardCharsets.US_ASCII);
    }

    public static boolean verifyUserPassword(
        String providedPassword,
        String securedPassword,
        String salt
    ) {
        boolean finalval = false;

        /* Generate New secure password with the same salt */
        String newSecurePassword = generateSecurePassword(providedPassword, salt);

        /* Check if two passwords are equal */
        finalval = newSecurePassword.equalsIgnoreCase(securedPassword);

        return finalval;
    }
}
