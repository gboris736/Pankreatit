package com.pancreatitis.modules.cloudstorage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.models.Update;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.authorization.AuthorizationModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.safety.SafetyModule;
import com.pancreatitis.modules.trainset.TrainingData;
import com.pancreatitis.modules.trainset.TrainingDataParser;
import javafx.util.Pair;
import okhttp3.*;

import javax.crypto.SecretKey;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class CloudStorageModule {
    private final String authToken = "y0__xC7koqFBxjIhTwg6O2OwBUwreWe7AeTLwUn1hxDxrN6Pp9NW10aTKlPGw";
    private static final String API_URL = "https://cloud-api.yandex.net/v1/disk/resources";
    private static CloudStorageModule instance;
    private SafetyModule safetyModule;
    private AuthorizationModule authorizationModule;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Константы для путей
    private static final String USERS_PATH = "/users/";
    private static final String UPDATE_PATH = "/update/";
    private static final String REGISTRATION_PATH = "/registration_requests/";
    private static final String ALGORITHM_FILE = "/algorithm.txt";

    private final OkHttpClient httpClient;

    public CloudStorageModule() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        executorService.execute(() -> {
            createFolder(USERS_PATH);
            createFolder(UPDATE_PATH);
            createFolder(REGISTRATION_PATH);
        });
    }

    public static CloudStorageModule getInstance() {
        if (instance == null) {
            synchronized (CloudStorageModule.class) {
                if (instance == null) {
                    instance = new CloudStorageModule();
                }
            }
        }
        return instance;
    }

    private SafetyModule getSafetyModule() {
        if (safetyModule == null) {
            safetyModule = SafetyModule.getInstance();
        }
        return safetyModule;
    }

    private AuthorizationModule getAuthorizationModule() {
        if (authorizationModule == null) {
            authorizationModule = AuthorizationModule.getInstance();
        }
        return authorizationModule;
    }

    // ==================== УНИВЕРСАЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Универсальный метод для выполнения HTTP запросов
     */
    private <T> T executeHttpRequest(Request request, HttpRequestProcessor<T> processor) throws Exception {
        try (Response response = httpClient.newCall(request).execute()) {
            return processor.process(response);
        }
    }

    /**
     * Универсальный метод для создания папки
     */
    private boolean createFolder(String path) {
        try {
            String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.name());
            String url = API_URL + "?path=" + encodedPath;

            Request request = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create(new byte[0], null))
                    .addHeader("Authorization", "OAuth " + authToken)
                    .build();

            return executeHttpRequest(request, response -> {
                int code = response.code();
                return code == 201 || code == 409;
            });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Универсальный метод для получения URL загрузки/скачивания
     */
    private String getResourceUrl(String path, String action, boolean overwrite) throws Exception {
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.name());
        String url = API_URL + "/" + action + "?path=" + encodedPath;
        if (action.equals("upload") && overwrite) {
            url += "&overwrite=true";
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "OAuth " + authToken)
                .build();

        return executeHttpRequest(request, response -> {
            if (response.code() == 200) {
                String json = response.body().string();
                return extractHrefFromJson(json);
            }
            return null;
        });
    }

    /**
     * Универсальный метод для скачивания файла
     */
    private byte[] downloadFile(String path) throws Exception {
        String downloadUrl = getResourceUrl(path, "download", false);
        if (downloadUrl == null) {
            throw new FileNotFoundException("File not found: " + path);
        }

        Request request = new Request.Builder()
                .url(downloadUrl)
                .get()
                .build();

        return executeHttpRequest(request, response -> {
            if (response.code() == 200) {
                return response.body().bytes();
            }
            throw new IOException("Failed to download file. Response code: " + response.code());
        });
    }

    /**
     * Универсальный метод для загрузки файла
     */
    private boolean uploadFile(String path, byte[] data) throws Exception {
        String uploadUrl = getResourceUrl(path, "upload", true);
        if (uploadUrl == null) {
            return false;
        }

        Request request = new Request.Builder()
                .url(uploadUrl)
                .put(RequestBody.create(data, MediaType.parse("application/octet-stream")))
                .build();

        return executeHttpRequest(request, response -> {
            int code = response.code();
            return code == 201 || code == 200;
        });
    }

    /**
     * Универсальный метод для получения списка элементов в папке
     */
    private List<FolderItem> listFolderContents(String folderPath) throws Exception {
        String encodedPath = URLEncoder.encode(folderPath, StandardCharsets.UTF_8.name());
        String url = API_URL + "?path=" + encodedPath;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "OAuth " + authToken)
                .build();

        return executeHttpRequest(request, response -> {
            List<FolderItem> items = new ArrayList<>();
            if (response.code() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body().byteStream());
                JsonNode itemsNode = rootNode.get("_embedded").get("items");

                if (itemsNode != null && itemsNode.isArray()) {
                    for (JsonNode item : itemsNode) {
                        items.add(new FolderItem(
                                item.get("name").asText(),
                                item.get("type").asText()
                        ));
                    }
                }
            }
            return items;
        });
    }

    /**
     * Универсальный метод для получения имен файлов в папке с фильтрацией
     */
    private List<String> getFileNamesInFolder(String folderPath, FileFilter filter) {
        try {
            return executorService.submit(() -> {
                List<FolderItem> items = listFolderContents(folderPath);
                List<String> result = new ArrayList<>();

                for (FolderItem item : items) {
                    if (filter == null || filter.accept(item)) {
                        result.add(item.getName());
                    }
                }
                return result;
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to list folder contents: " + folderPath, e);
        }
    }

    /**
     * Универсальный метод для скачивания и парсинга JSON файлов
     */
    private <T> T downloadAndParseJson(String path, Class<T> valueType) throws Exception {
        byte[] jsonBytes = downloadFile(path);
        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        return objectMapper.readValue(json, valueType);
    }

    /**
     * Универсальный метод для удаления файла
     */
    private boolean deleteFile(String path) {
        try {
            String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.name());
            String url = API_URL + "?path=" + encodedPath + "&permanently=true";

            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Authorization", "OAuth " + authToken)
                    .build();

            return executorService.submit(() ->
                    executeHttpRequest(request, response -> {
                        int code = response.code();
                        return code == 202 || code == 204;
                    })
            ).get();
        } catch (Exception e) {
            System.err.println("Ошибка при удалении файла " + path + ": " + e.getMessage());
            return false;
        }
    }

    // ==================== СПЕЦИАЛИЗИРОВАННЫЕ МЕТОДЫ ====================

    public boolean createUserFolder(String login) {
        return createFolder("/users/" + login);
    }

    public boolean isUserFolderExists(String login) {
        try {
            return executorService.submit(() -> {
                String path = USERS_PATH + login;
                List<FolderItem> items = listFolderContents(USERS_PATH);
                return items.stream().anyMatch(item ->
                        item.isDirectory() && item.getName().equals(login));
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    public byte[] downloadKey(String login, String user) throws Exception {
        return downloadFile(USERS_PATH + login + "/key_" + user + ".enc");
    }

    public boolean uploadUserKey(String login, byte[] keyBytes, String keyName) throws Exception {
        String fileName = keyName + ".enc";
        return uploadFile(USERS_PATH + login + "/" + fileName, keyBytes);
    }

    // ==================== НОВЫЕ МЕТОДЫ ДЛЯ ОБНОВЛЕНИЙ (UUID) ====================

    /**
     * Загружает зашифрованное обновление в облако.
     * Имя файла формируется как login_uuid.json.
     *
     * @param update   объект обновления (должен содержать login, dateUpload, hashSum)
     * @param adminKey административный ключ врача для шифрования
     * @param login    логин врача (используется в имени файла)
     * @return UUID часть имени файла или null при ошибке
     */
    public String uploadEncryptedUpdate(Update update, SecretKey adminKey, String login) throws Exception {
        String jsonData = update.toJson();
        String encryptedJson = getSafetyModule().encryptString(jsonData, adminKey);
        byte[] encryptedBytes = encryptedJson.getBytes(StandardCharsets.UTF_8);

        String uuid = UUID.randomUUID().toString();
        String fileName = login + "_" + uuid + ".json";
        String filePath = UPDATE_PATH + fileName;

        boolean success = uploadFile(filePath, encryptedBytes);
        return success ? uuid : null;
    }

    /**
     * Скачивает и расшифровывает обновление по полному имени файла.
     *
     * @param fileName    полное имя файла (например, "doctor_123e4567-e89b-12d3-a456-426614174000.json")
     * @return объект Update
     */
    public Update downloadEncryptedUpdate(String fileName, String doctorLogin) throws Exception {
        String filePath = UPDATE_PATH + fileName;
        byte[] encryptedBytes = downloadFile(filePath);
        String encryptedJson = new String(encryptedBytes, StandardCharsets.UTF_8);

        SecretKey keyAdmin = getAuthorizationModule().authenticateForAdmin(doctorLogin);
        String decryptedJson = getSafetyModule().decryptString(encryptedJson, keyAdmin);

        return objectMapper.readValue(decryptedJson, Update.class);
    }
    /**
     * Возвращает список имён файлов обновлений (с расширением .json)
     */
    public List<String> getUpdateFileNames() {
        return getFileNamesInFolder(UPDATE_PATH,
                item -> item.getName().endsWith(".json") && item.getName().contains("_"));
    }

    /**
     * Асинхронно получает список имён файлов обновлений
     */
    public CompletableFuture<List<String>> getUpdateFileNamesAsync() {
        return CompletableFuture.supplyAsync(this::getUpdateFileNames, executorService);
    }

    /**
     * Удаляет файл обновления по его имени
     */
    public boolean deleteUpdateFile(String fileName) {
        return deleteFile(UPDATE_PATH + fileName);
    }

    // ==================== МЕТОДЫ ДЛЯ РЕГИСТРАЦИИ ====================

    public boolean uploadRegistrationRequest(RegistrationForm registrationForm) throws Exception {
        String jsonData = registrationForm.toJson();
        String fileName = registrationForm.getLogin() + ".json";
        return uploadFile(REGISTRATION_PATH + fileName, jsonData.getBytes(StandardCharsets.UTF_8));
    }

    public RegistrationForm downloadRegistrationForm(String login) {
        try {
            return downloadAndParseJson(REGISTRATION_PATH + login + ".json", RegistrationForm.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download registration form for login: " + login, e);
        }
    }

    public List<RegistrationForm> getAllRegistrationForms() {
        List<RegistrationForm> forms = new ArrayList<>();
        List<String> logins = getRegistrationFormLogins();
        for (String login : logins) {
            forms.add(downloadRegistrationForm(login));
        }
        return forms;
    }

    private List<String> getRegistrationFormLogins() {
        return getFileNamesInFolder(REGISTRATION_PATH, item -> item.getName().endsWith(".json"));
    }

    public boolean deleteRegistrationRequest(String login) {
        return deleteFile(REGISTRATION_PATH + login + ".json");
    }

    // ==================== МЕТОДЫ ДЛЯ ПОЛЬЗОВАТЕЛЕЙ ====================

    public User downloadUserInfo(String login) {
        try {
            String filePath = USERS_PATH + login + "/user_info.json";
            byte[] jsonBytes = downloadFile(filePath);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);
            return parseUserFromJson(json, login);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download user info for login: " + login, e);
        }
    }

    public boolean uploadUserInfo(RegistrationForm registrationForm) throws Exception {
        String login = registrationForm.getLogin();
        if (login == null || login.isEmpty()) {
            return false;
        }
        String jsonData = registrationForm.toJson();
        return uploadFile(USERS_PATH + login + "/user_info.json", jsonData.getBytes(StandardCharsets.UTF_8));
    }

    private User parseUserFromJson(String json, String login) throws Exception {
        JsonNode rootNode = objectMapper.readTree(json);
        User user = new User();

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
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        List<String> logins = getUserLogins();
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        for (String login : logins) {
            User user = downloadUserInfo(login);
            Doctor doctor = databaseModule.getDoctorByFio(user.getFullName());
            user.setDoctor(doctor);
            users.add(user);
        }
        return users;
    }

    private List<String> getUserLogins() {
        return getFileNamesInFolder(USERS_PATH, FolderItem::isDirectory);
    }

    // ==================== МЕТОДЫ ДЛЯ ОБУЧАЮЩИХ ДАННЫХ ====================

    public byte[] downloadTrainingData() throws Exception {
        return downloadFile(ALGORITHM_FILE);
    }

    public boolean uploadTrainingData(TrainingData trainingData) {
        try {
            String textData = TrainingDataParser.serializeToTextFormat(trainingData);
            return uploadFile(ALGORITHM_FILE, textData.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private String extractHrefFromJson(String json) {
        int hrefIndex = json.indexOf("\"href\":\"");
        if (hrefIndex != -1) {
            int start = hrefIndex + 8;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }
        return null;
    }

    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ ====================

    @FunctionalInterface
    private interface HttpRequestProcessor<T> {
        T process(Response response) throws Exception;
    }

    @FunctionalInterface
    private interface FileFilter {
        boolean accept(FolderItem item);
    }

    private static class FolderItem {
        private final String name;
        private final String type;

        public FolderItem(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() { return name; }
        public boolean isDirectory() { return "dir".equals(type); }
        public boolean isFile() { return "file".equals(type); }
    }
}