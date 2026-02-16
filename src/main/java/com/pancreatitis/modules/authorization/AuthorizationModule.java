package com.pancreatitis.modules.authorization;

import com.pancreatitis.models.User;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;
import com.pancreatitis.modules.safety.SafetyModule;

import javax.crypto.SecretKey;

public class AuthorizationModule {
    private static CloudStorageModule cloudStorageModule;
    private static LocalStorageModule localStorageModule;
    private static SafetyModule safetyModule;
    private static AuthorizationModule instance;

    private AuthorizationModule(){
        cloudStorageModule = CloudStorageModule.getInstance();
        localStorageModule = LocalStorageModule.getInstance();
        safetyModule = SafetyModule.getInstance();
    }

    public static AuthorizationModule getInstance(){
        if (instance == null) {
            instance = new AuthorizationModule();
        }
        return instance;
    }

    public SecretKey authenticateUser(String login, String password) throws Exception {

        if (!cloudStorageModule.isUserFolderExists(login) && !localStorageModule.isUserFolderExists(login)){
            return null;
        }

        byte[] encrypted_key = getKey(login);
        SecretKey key = safetyModule.decryptKey(encrypted_key, password);
        return key;
    }

    private byte[] getKey(String login) throws Exception {
        if (!localStorageModule.isUserKeyExists(login)) {
            byte[] key = cloudStorageModule.downloadUserKey(login);
            User user_info = cloudStorageModule.downloadUserInfo(login);
            localStorageModule.createUserFolder(login);
            localStorageModule.saveLocalUserKey(login, key);
            localStorageModule.saveUserInfo(user_info);
            return key;
        } else {
            byte[] key = localStorageModule.downloadUserKey(login);
            return key;
        }
    }
}
