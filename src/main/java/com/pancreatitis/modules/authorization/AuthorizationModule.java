//package com.pancreatitis.modules.authorization;
//
//import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
//import com.pancreatitis.modules.localstorage.LocalStorageModule;
//import com.pancreatitis.modules.safety.SafetyModule;
//
//import javax.crypto.SecretKey;
//
//public class AuthorizationModule {
//    private static CloudStorageModule cloudStorageModule;
//    private static LocalStorageModule localStorageModule;
//    private static SafetyModule safetyModule;
//    private static AuthorizationModule instance;
//
//    private AuthorizationModule() {
//        cloudStorageModule = CloudStorageModule.getInstance();
//        localStorageModule = LocalStorageModule.getInstance();
//        safetyModule = SafetyModule.getInstance();
//    }
//
//    public static AuthorizationModule getInstance() {
//        if (instance == null) {
//            instance = new AuthorizationModule();
//        }
//        return instance;
//    }
//
//    /**
//     * Аутентификация обычного пользователя
//     */
//    public SecretKey authenticateUser(String login, String password) throws Exception {
//        if (!isUserExists(login)) {
//            return null;
//        }
//
//        byte[] encryptedKey = getUserKey(login);
//        return safetyModule.decryptKey(encryptedKey, password);
//    }
//
//    /**
//     * Аутентификация администратора для доступа к данным пользователя
//     */
//    public SecretKey authenticateForAdmin(String login) throws Exception {
//        if (!isUserExists(login)) {
//            return null;
//        }
//
//        byte[] encryptedKey = getAdminKey(login);
//        return safetyModule.decryptKey(encryptedKey, safetyModule.getAdminPassword());
//    }
//
//    /**
//     * Проверка существования пользователя
//     */
//    private boolean isUserExists(String login) {
//        return false;
////        return cloudStorageModule.isUserFolderExists(login) ||
////                localStorageModule.isUserFolderExists(login);
//    }
//
////    /**
////     * Получение пользовательского ключа (с синхронизацией из облака при необходимости)
////     */
////    private byte[] getUserKey(String login) throws Exception {
////        // Проверяем наличие локального пользовательского ключа
////        if (localStorageModule.isUserKeyExists(login)) {
////            return localStorageModule.downloadUserKey(login);
////        }
////    }
////
////    /**
////     * Получение административного ключа (с синхронизацией из облака при необходимости)
////     */
////    private byte[] getAdminKey(String login) throws Exception {
////        // Проверяем наличие локального административного ключа
////        if (localStorageModule.isAdminKeyExists(login)) {
////            return localStorageModule.downloadAdminKey(login);
////        }
////    }
//}