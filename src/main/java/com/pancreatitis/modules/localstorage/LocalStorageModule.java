package com.pancreatitis.modules.localstorage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LocalStorageModule {
    private static DiskStorageControl diskStorageControl;
    private static LocalStorageModule instance;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Базовый путь для хранения данных
    private File storageBaseDir;
    private static final String USERS_DIR = "users";

    // Константы для имен ключей
    private static final String USER_KEY_FILE = "key_user.enc";
    private static final String ADMIN_KEY_FILE = "key_admin.enc";
    private static final String USER_INFO_FILE = "user_info.json";

    private static final String ED25519_PRIVATE_KEY_FILE = "ed25519_private.key";
    private static final String ED25519_PUBLIC_KEY_FILE = "ed25519_public.key";

    private LocalStorageModule() {
        diskStorageControl = DiskStorageControl.getInstance();
        initializeStorage();

//        // В любом месте, например, в main() или утилите инициализации
//        try{
//            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("Ed25519");
//            KeyPair pair = keyGen.generateKeyPair();
//
//            byte[] privateKeyBytes = pair.getPrivate().getEncoded(); // PKCS#8
//            byte[] publicKeyBytes = pair.getPublic().getEncoded();   // X.509
//
//            saveEd25519PrivateKey(privateKeyBytes);
//            saveEd25519PublicKey(publicKeyBytes);
//        } catch (Exception e){
//
//        }
    }

    public static LocalStorageModule getInstance() {
        if (instance == null) {
            synchronized (LocalStorageModule.class) {
                if (instance == null) {
                    instance = new LocalStorageModule();
                }
            }
        }
        return instance;
    }

    private void initializeStorage() {
        executorService.execute(() -> createFolder(USERS_DIR));
    }

    private File getStorageBaseDir() {
        if (storageBaseDir == null) {
            storageBaseDir = new File(diskStorageControl.getAppDir().toUri());
        }
        return storageBaseDir;
    }

    private boolean createFolder(String folderName) {
        File folder = new File(getStorageBaseDir(), folderName);
        if (!folder.exists()) {
            return folder.mkdirs();
        }
        return true;
    }

    public boolean createUserFolder(String login) {
        try {
            Future<Boolean> future = executorService.submit(() -> {
                File userFolder = new File(getFolder(USERS_DIR), login);
                if (userFolder.exists()) {
                    return true;
                }
                boolean created = userFolder.mkdirs();
                if (created) {
                    System.out.println("Папка пользователя создана: " + userFolder.getAbsolutePath());
                }
                return created;
            });
            return future.get();
        } catch (Exception e) {
            return false;
        }
    }

    private File getFolder(String folderName) {
        return new File(getStorageBaseDir(), folderName);
    }

    public boolean isUserFolderExists(String login) {
        try {
            Future<Boolean> future = executorService.submit(() -> {
                File userFolder = new File(getFolder(USERS_DIR), login);
                return userFolder.exists() && userFolder.isDirectory();
            });
            return future.get();
        } catch (Exception e) {
            return false;
        }
    }

    // Сохранение приватного ключа
    public boolean saveEd25519PrivateKey(byte[] keyBytes) throws Exception {
        File keyFile = new File(getStorageBaseDir(), ED25519_PRIVATE_KEY_FILE);
        return writeFile(keyFile, keyBytes);
    }

    // Сохранение публичного ключа
    public boolean saveEd25519PublicKey(byte[] keyBytes) throws Exception {
        File keyFile = new File(getStorageBaseDir(), ED25519_PUBLIC_KEY_FILE);
        return writeFile(keyFile, keyBytes);
    }

    // Загрузка приватного ключа
    public PrivateKey loadEd25519PrivateKey() throws Exception {
        File keyFile = new File(getStorageBaseDir(), ED25519_PRIVATE_KEY_FILE);
        if (!keyFile.exists()) {
            throw new FileNotFoundException("Ed25519 private key not found");
        }
        byte[] keyBytes = readFileAsBytes(keyFile);
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    // Загрузка публичного ключа
    public PublicKey loadEd25519PublicKey() throws Exception {
        File keyFile = new File(getStorageBaseDir(), ED25519_PUBLIC_KEY_FILE);
        if (!keyFile.exists()) {
            throw new FileNotFoundException("Ed25519 public key not found");
        }
        byte[] keyBytes = readFileAsBytes(keyFile);
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    // ==================== МЕТОДЫ ДЛЯ РАБОТЫ С КЛЮЧАМИ ====================

    /**
     * Загрузка пользовательского ключа
     */
    public byte[] downloadUserKey(String login) throws Exception {
        return downloadKey(login, USER_KEY_FILE);
    }

    /**
     * Загрузка административного ключа
     */
    public byte[] downloadAdminKey(String login) throws Exception {
        return downloadKey(login, ADMIN_KEY_FILE);
    }

    private byte[] downloadKey(String login, String keyFileName) throws Exception {
        Callable<byte[]> task = () -> {
            File keyFile = new File(new File(getFolder(USERS_DIR), login), keyFileName);

            if (!keyFile.exists()) {
                throw new FileNotFoundException("Файл ключа не найден: " + keyFile.getAbsolutePath());
            }

            return readFileAsBytes(keyFile);
        };

        return executorService.submit(task).get();
    }

    /**
     * Сохранение пользовательского ключа
     */
    public boolean saveUserKey(String login, byte[] keyData) throws Exception {
        return saveKey(login, USER_KEY_FILE, keyData);
    }

    /**
     * Сохранение административного ключа
     */
    public boolean saveAdminKey(String login, byte[] keyData) throws Exception {
        return saveKey(login, ADMIN_KEY_FILE, keyData);
    }

    private boolean saveKey(String login, String keyFileName, byte[] keyData) throws Exception {
        File userFolder = new File(getFolder(USERS_DIR), login);

        if (!userFolder.exists()) {
            if (!userFolder.mkdirs()) {
                throw new IOException("Не удалось создать папку пользователя: " + login);
            }
        }

        File keyFile = new File(userFolder, keyFileName);

        try (FileOutputStream fos = new FileOutputStream(keyFile)) {
            fos.write(keyData);
            fos.flush();
            return true;
        } catch (IOException e) {
            throw new IOException("Ошибка при сохранении ключа: " + e.getMessage(), e);
        }
    }

    /**
     * Проверка существования пользовательского ключа
     */
    public boolean isUserKeyExists(String login) throws Exception {
        return isKeyExists(login, USER_KEY_FILE);
    }

    /**
     * Проверка существования административного ключа
     */
    public boolean isAdminKeyExists(String login) throws Exception {
        return isKeyExists(login, ADMIN_KEY_FILE);
    }

    private boolean isKeyExists(String login, String keyFileName) throws Exception {
        Callable<Boolean> task = () -> {
            File keyFile = new File(new File(getFolder(USERS_DIR), login), keyFileName);
            return keyFile.exists();
        };
        return executorService.submit(task).get();
    }

    // ==================== МЕТОДЫ ДЛЯ РАБОТЫ С ИНФОРМАЦИЕЙ ПОЛЬЗОВАТЕЛЯ ====================

    public User downloadUserInfo(String login) throws Exception {
        Callable<User> task = () -> {
            File userInfoFile = new File(new File(getFolder(USERS_DIR), login), USER_INFO_FILE);

            if (!userInfoFile.exists()) {
                throw new FileNotFoundException("Информация о пользователе не найдена: " + userInfoFile.getAbsolutePath());
            }

            byte[] jsonBytes = readFileAsBytes(userInfoFile);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            return parseUserFromJson(json, login);
        };

        return executorService.submit(task).get();
    }

    public boolean saveUserInfo(User user) throws Exception {
        Callable<Boolean> task = () -> {
            if (user == null || user.getLogin() == null || user.getLogin().isEmpty()) {
                throw new IllegalArgumentException("User or login cannot be null or empty");
            }

            File userFolder = new File(getFolder(USERS_DIR), user.getLogin());
            if (!userFolder.exists()) {
                if (!userFolder.mkdirs()) {
                    throw new IOException("Failed to create user folder: " + user.getLogin());
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode userJson = mapper.createObjectNode();

            if (user.getEmail() != null) {
                userJson.put("email", user.getEmail());
            }

            if (user.getDoctor() != null && user.getDoctor().getFio() != null) {
                userJson.put("fullName", user.getDoctor().getFio());
            }

            if (user.getPhone() != null) {
                userJson.put("phone", user.getPhone());
            }

            userJson.put("login", user.getLogin());
            userJson.put("updatedAt", LocalDateTime.now().format(formatter));

            String jsonData = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userJson);
            File userInfoFile = new File(userFolder, USER_INFO_FILE);
            return writeFile(userInfoFile, jsonData.getBytes(StandardCharsets.UTF_8));
        };

        return executorService.submit(task).get();
    }

    private User parseUserFromJson(String json, String login) throws Exception {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);

            User user = User.getInstance();

            if (rootNode.has("email")) {
                user.setEmail(rootNode.get("email").asText());
            }

            if (rootNode.has("fullName")) {
                Doctor doctor = new Doctor();
                doctor.setFio(rootNode.get("fullName").asText());
                user.setDoctor(doctor);
            }

            if (rootNode.has("phone")) {
                user.setPhone(rootNode.get("phone").asText());
            }

            user.setLogin(login);

            return user;
        } catch (Exception e) {
            throw new IOException("Failed to parse user JSON: " + e.getMessage(), e);
        }
    }

    // ==================== МЕТОДЫ ДЛЯ РАБОТЫ С ТРЕНИРОВОЧНЫМИ ДАННЫМИ ====================

    public byte[] getTrainingData() throws Exception {
        Callable<byte[]> task = () -> {
            File algorithmFile = new File(getStorageBaseDir(), "algorithm.txt");

            if (!algorithmFile.exists()) {
                throw new FileNotFoundException("File not found: " + algorithmFile.getAbsolutePath());
            }

            return readFileAsBytes(algorithmFile);
        };

        return executorService.submit(task).get();
    }

    public byte[] getTrainingData(String version) {
        try {
            Callable<byte[]> task = () -> {
                File algorithmFile = new File(getStorageBaseDir(), version);

                if (!algorithmFile.exists()) {
                    throw new FileNotFoundException("File not found: " + algorithmFile.getAbsolutePath());
                }

                return readFileAsBytes(algorithmFile);
            };

            return executorService.submit(task).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isTrainingData() throws Exception {
        Callable<Boolean> task = () -> {
            File keyFile = new File(getStorageBaseDir(), "algorithm.txt");
            return keyFile.exists();
        };

        return executorService.submit(task).get();
    }

    public void downloadAlgorithmFile(byte[] algorithmData) throws Exception {
        Callable<Boolean> task = () -> {
            File algorithmFile = new File(getStorageBaseDir(), "algorithm.txt");
            return writeFile(algorithmFile, algorithmData);
        };

        executorService.submit(task).get();
    }

    public String getPathAlgorithmFile() {
        File algorithmFile = new File(getStorageBaseDir(), "algorithm.txt");
        String filePath = algorithmFile.getAbsolutePath();
        return filePath;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private byte[] readFileAsBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            return bos.toByteArray();
        }
    }

    private boolean writeFile(File file, byte[] data) throws IOException {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getUserFolderPath(String login) {
        File userFolder = new File(getFolder(USERS_DIR), login);
        return userFolder.getAbsolutePath();
    }

    public boolean isStorageAvailable() {
        return getStorageBaseDir() != null && getStorageBaseDir().canWrite();
    }

    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    public boolean deleteUserData(String login) throws Exception {
        Callable<Boolean> task = () -> {
            File userFolder = new File(getFolder(USERS_DIR), login);
            return deleteRecursive(userFolder);
        };

        return executorService.submit(task).get();
    }

    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }
}