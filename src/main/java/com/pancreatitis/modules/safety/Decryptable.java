package com.pancreatitis.modules.safety;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public interface Decryptable {

    static SecretKey decryptWithPassword(byte[] encryptedKey, String password) throws Exception {
        try {
            SecretKey passwordKey = Encryptable.deriveKeyFromPassword(password);
            return decryptWithKey(encryptedKey, passwordKey);
        } catch (Exception e) {
            return null;
        }
    }

    static SecretKey decryptWithKey(byte[] encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedKeyBytes = cipher.doFinal(encryptedData);
        return new SecretKeySpec(decryptedKeyBytes, "AES");
    }

    static String decryptString(String encryptedText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, "UTF-8");
    }
}