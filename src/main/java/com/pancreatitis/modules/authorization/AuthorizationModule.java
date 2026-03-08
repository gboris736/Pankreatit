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

    private AuthorizationModule() {
        cloudStorageModule = CloudStorageModule.getInstance();
        localStorageModule = LocalStorageModule.getInstance();
        safetyModule = SafetyModule.getInstance();
    }

    public static AuthorizationModule getInstance() {
        if (instance == null) {
            instance = new AuthorizationModule();
        }
        return instance;
    }

    /**
     * Аутентификация обычного пользователя
     */
    public SecretKey authenticateUser(String login, String password) throws Exception {
        if (!isUserExists(login)) {
            return null;
        }

        byte[] encryptedKey = getUserKey(login);
        return safetyModule.decryptKey(encryptedKey, password);
    }

    /**
     * Аутентификация администратора для доступа к данным пользователя
     */
    public SecretKey authenticateForAdmin(String login) throws Exception {
        if (!isUserExists(login)) {
            return null;
        }

        byte[] encryptedKey = getAdminKey(login);
        return safetyModule.decryptKey(encryptedKey, safetyModule.getAdminPassword());
    }

    /**
     * Проверка существования пользователя
     */
    private boolean isUserExists(String login) {
        return cloudStorageModule.isUserFolderExists(login) ||
                localStorageModule.isUserFolderExists(login);
    }

    /**
     * Получение пользовательского ключа (с синхронизацией из облака при необходимости)
     */
    private byte[] getUserKey(String login) throws Exception {
        // Проверяем наличие локального пользовательского ключа
        if (localStorageModule.isUserKeyExists(login)) {
            return localStorageModule.downloadUserKey(login);
        }

        // Если локального нет, загружаем из облака
        return syncUserDataFromCloud(login, false);
    }

    /**
     * Получение административного ключа (с синхронизацией из облака при необходимости)
     */
    private byte[] getAdminKey(String login) throws Exception {
        // Проверяем наличие локального административного ключа
        if (localStorageModule.isAdminKeyExists(login)) {
            return localStorageModule.downloadAdminKey(login);
        }

        // Если локального нет, загружаем из облака
        return syncUserDataFromCloud(login, true);
    }

    /**
     * Синхронизация данных пользователя из облака в локальное хранилище
     * @param login логин пользователя
     * @param isAdmin true - загрузить административный ключ, false - пользовательский
     * @return загруженный ключ
     */
    private byte[] syncUserDataFromCloud(String login, boolean isAdmin) throws Exception {
        // Загружаем ключ из облака
        byte[] key;
        if (isAdmin) {
            key = cloudStorageModule.downloadKey(login, "admin");
        } else {
            key = cloudStorageModule.downloadKey(login, "user");
        }

        // Загружаем информацию о пользователе
        User userInfo = cloudStorageModule.downloadUserInfo(login);

        // Создаем локальную папку пользователя
        localStorageModule.createUserFolder(login);

        // Сохраняем ключ в локальное хранилище
        if (isAdmin) {
            localStorageModule.saveAdminKey(login, key);
        } else {
            localStorageModule.saveUserKey(login, key);
        }

        // Сохраняем информацию о пользователе
        localStorageModule.saveUserInfo(userInfo);

        return key;
    }
}