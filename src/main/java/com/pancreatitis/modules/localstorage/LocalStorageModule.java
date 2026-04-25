package com.pancreatitis.modules.localstorage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pancreatitis.models.Doctor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private static final String USERS_DIR = "users";

    // Константы для имен ключей
    private static final String USER_KEY_FILE = "key_user.enc";
    private static final String ADMIN_KEY_FILE = "key_admin.enc";

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