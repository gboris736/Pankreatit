package com.pancreatitis.pankreat2.modules.authorization;

import android.content.Context;

import com.pancreatitis.pankreat2.models.User;
import com.pancreatitis.pankreat2.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.pankreat2.modules.localstorage.LocalStorageModule;
import com.pancreatitis.pankreat2.modules.safety.SafetyModule;

import javax.crypto.SecretKey;

public class AuthorizationModule {
    private static CloudStorageModule cloudStorageModule;
    private static LocalStorageModule localStorageModule;
    private static SafetyModule safetyModule;
    private static AuthorizationModule instance;
    private Context context;
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

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
    public SecretKey authenticateUser(String login, String password) throws Exception {
        localStorageModule.setContext(context);

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
