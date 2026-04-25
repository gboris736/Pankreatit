package com.pancreatitis.modules.localstorage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pancreatitis.models.Doctor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private File algorithmDir;
    private static final String USERS_DIR = "users";

    // Константы для имен ключей
    private static final String USER_KEY_FILE = "key_user.enc";
    private static final String ADMIN_KEY_FILE = "key_admin.enc";

    private static final String ED25519_PRIVATE_KEY_FILE = "ed25519_private.key";
    private static final String ED25519_PUBLIC_KEY_FILE = "ed25519_public.key";

    private LocalStorageModule() {
        diskStorageControl = DiskStorageControl.getInstance();
        initializeStorage();
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

    public List<String> getAllUserLogins() throws Exception {
        Callable<List<String>> task = () -> {
            File usersFolder = getFolder(USERS_DIR);
            List<String> logins = new ArrayList<>();

            if (!usersFolder.exists() || !usersFolder.isDirectory()) {
                return logins; // пустой список, если папки users нет
            }

            File[] files = usersFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        logins.add(file.getName());
                    }
                }
            }
            return logins;
        };

        return executorService.submit(task).get();
    }

    // ==================== МЕТОДЫ ДЛЯ РАБОТЫ С КЛЮЧАМИ ED25519 ====================

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

    // ==================== МЕТОДЫ ОБУЧАЮЕЙ ВЫБОРКИ ====================

    private File getAlgorithmDir() {
        if (algorithmDir == null) {
            algorithmDir = diskStorageControl.getAlgDir().toFile();
            if (!algorithmDir.exists()) {
                algorithmDir.mkdirs();
            }
        }
        return algorithmDir;
    }

    public List<String> listAlgorithmFiles() {
        List<String> names = new ArrayList<>();
        try {
            Callable<List<String>> task = () -> {
                File dir = getAlgorithmDir();
                File[] files = dir.listFiles((d, name) -> name.matches("algorithm_\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}\\.txt"));
                if (files != null) {
                    for (File f : files) {
                        names.add(f.getName());
                    }
                }
                names.sort(Comparator.reverseOrder()); // свежие сверху
                return names;
            };
            return executorService.submit(task).get();
        } catch (Exception e){
            return names;
        }
    }

    // Чтение произвольного файла алгоритма
    public byte[] readAlgorithmFile(String fileName) throws Exception {
        Callable<byte[]> task = () -> {
            File file = new File(getAlgorithmDir(), fileName);
            if (!file.exists()) {
                throw new FileNotFoundException("Algorithm file not found: " + file.getAbsolutePath());
            }
            return readFileAsBytes(file);
        };
        return executorService.submit(task).get();
    }

    // Запись нового файла алгоритма
    public boolean writeAlgorithmFile(String fileName, byte[] data) throws Exception {
        Callable<Boolean> task = () -> writeFile(new File(getAlgorithmDir(), fileName), data);
        return executorService.submit(task).get();
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