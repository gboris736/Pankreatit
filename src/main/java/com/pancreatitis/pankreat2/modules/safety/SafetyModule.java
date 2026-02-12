package com.pancreatitis.pankreat2.modules.safety;

import com.pancreatitis.pankreat2.models.Patient;
import com.pancreatitis.pankreat2.models.Questionnaire;
import com.pancreatitis.pankreat2.modules.authorization.AuthorizationModule;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

public class SafetyModule {
    private static String ADMIN_PASSWORD = "7oZw18g*2@Mh1aij1+";
    private static SafetyModule instance;
    public static SafetyModule getInstance(){
        if (instance == null) {
            instance = new SafetyModule();
        }
        return instance;
    }

    public String getAdminPassword() {
        return ADMIN_PASSWORD;
    }

    public String decryptString(String encryptedText, SecretKey key) throws Exception {
        return Decryptable.decryptString(encryptedText, key);
    }

    public String encryptString(String plainText, SecretKey key) throws Exception {
        return Encryptable.encryptString(plainText, key);
    }

    public SecretKey decryptKey(byte[] encrypted_key, String password) throws Exception {
        return Decryptable.decryptWithPassword(encrypted_key, password);
    }

    public byte[] encryptKey(SecretKey decrypted_key, String password) throws Exception {
        return Encryptable.encryptWithPassword(decrypted_key, password);
    }

    public SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        return keyGen.generateKey();
    }
}
