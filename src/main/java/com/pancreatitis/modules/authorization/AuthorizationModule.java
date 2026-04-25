package com.pancreatitis.modules.authorization;

import com.pancreatitis.modules.localstorage.LocalStorageModule;
import com.pancreatitis.modules.safety.SafetyModule;

import javax.crypto.SecretKey;

public class AuthorizationModule {
    private static LocalStorageModule localStorageModule;
    private static SafetyModule safetyModule;
    private static AuthorizationModule instance;

    private AuthorizationModule() {
        safetyModule = SafetyModule.getInstance();
    }

    public static AuthorizationModule getInstance() {
        if (instance == null) {
            instance = new AuthorizationModule();
        }
        return instance;
    }

    public LocalStorageModule getLocalStorageModule() {
        if (localStorageModule == null) {
            localStorageModule = LocalStorageModule.getInstance();
        }
        return localStorageModule;
    }

    /**
     * Аутентификация обычного пользователя
     */
    public SecretKey authenticateUser(String login, String password) throws Exception {
        if (!isUserExists(login)) {
            return null;
        }

        byte[] encryptedKey = getLocalStorageModule().downloadUserKey(login);
        return safetyModule.decryptKey(encryptedKey, password);
    }

    /**
     * Аутентификация администратора для доступа к данным пользователя
     */
    public SecretKey authenticateForAdmin(String login) throws Exception {
        if (!isUserExists(login)) {
            return null;
        }

        byte[] encryptedKey = getLocalStorageModule().downloadAdminKey(login);
        return safetyModule.decryptKey(encryptedKey, safetyModule.getAdminPassword());
    }

    /**
     * Проверка существования пользователя
     */
    private boolean isUserExists(String login) {
        return getLocalStorageModule().isUserFolderExists(login);
    }
}