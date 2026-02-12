package com.pancreatitis.pankreat2.modules.safety;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public interface Encryptable {
    static byte[] encryptWithPassword(SecretKey key, String password) throws Exception {
        SecretKey passwordKey = deriveKeyFromPassword(password);
        return encryptWithKey(key, passwordKey);
    }

    static byte[] encryptWithKey(SecretKey data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data.getEncoded());
    }

    static SecretKey deriveKeyFromPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(password.getBytes());
        return new SecretKeySpec(keyBytes, 0, 16, "AES");
    }

    static String encryptString(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
}