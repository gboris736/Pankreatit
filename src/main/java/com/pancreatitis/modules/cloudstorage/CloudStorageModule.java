package com.pancreatitis.modules.cloudstorage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.models.Update;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.trainset.TrainingData;
import com.pancreatitis.modules.trainset.TrainingDataParser;
import javafx.util.Pair;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudStorageModule {
    private final String authToken = "y0__xC7koqFBxjIhTwg6O2OwBUwreWe7AeTLwUn1hxDxrN6Pp9NW10aTKlPGw";
    private static final String API_URL = "https://cloud-api.yandex.net/v1/disk/resources";
    private static CloudStorageModule instance;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Константы для путей
    private static final String USERS_PATH = "/users/";
    private static final String UPDATE_PATH = "/update/";
    private static final String REGISTRATION_PATH = "/registration_requests/";
    private static final String ALGORITHM_FILE = "/algorithm.txt";

    private CloudStorageModule() {
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

    // ==================== УНИВЕРСАЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Универсальный метод для выполнения HTTP запросов
     */
    private <T> T executeHttpRequest(String urlStr, String method, String authToken,
                                     HttpRequestProcessor<T> processor) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(urlStr).openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "OAuth " + authToken);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            return processor.process(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Универсальный метод для создания папки
     */
    private boolean createFolder(String path) {
        try {
            String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
            String url = API_URL + "?path=" + encodedPath;

            return executeHttpRequest(url, "PUT", authToken, connection -> {
                int responseCode = connection.getResponseCode();
                return responseCode == 201 || responseCode == 409;
            });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Универсальный метод для получения URL загрузки/скачивания
     */
    private String getResourceUrl(String path, String action, boolean overwrite) throws Exception {
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
        String url = API_URL + "/" + action + "?path=" + encodedPath;
        if (action.equals("upload") && overwrite) {
            url += "&overwrite=true";
        }

        return executeHttpRequest(url, "GET", authToken, connection -> {
            if (connection.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return extractHrefFromJson(response.toString());
                }
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

        return executeHttpRequest(downloadUrl, "GET", null, connection -> {
            if (connection.getResponseCode() == 200) {
                try (InputStream inputStream = connection.getInputStream();
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    return outputStream.toByteArray();
                }
            }
            throw new IOException("Failed to download file. Response code: " + connection.getResponseCode());
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

        return executeHttpRequest(uploadUrl, "PUT", null, connection -> {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(data);
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            return responseCode == 201 || responseCode == 200;
        });
    }

    /**
     * Универсальный метод для получения списка элементов в папке
     */
    private List<FolderItem> listFolderContents(String folderPath) throws Exception {
        String encodedPath = URLEncoder.encode(folderPath, StandardCharsets.UTF_8.toString());
        String url = API_URL + "?path=" + encodedPath;

        return executeHttpRequest(url, "GET", authToken, connection -> {
            List<FolderItem> items = new ArrayList<>();
            if (connection.getResponseCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(connection.getInputStream());
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
            String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
            String url = API_URL + "?path=" + encodedPath + "&permanently=true";

            return executorService.submit(() ->
                    executeHttpRequest(url, "DELETE", authToken, connection -> {
                        int responseCode = connection.getResponseCode();
                        return responseCode == 202 || responseCode == 204;
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

    public byte[] downloadUserKey(String login) throws Exception {
        return downloadFile(USERS_PATH + login + "/key_user.enc");
    }

    public boolean uploadUserKey(String login, byte[] keyBytes, String keyName) throws Exception {
        String fileName = keyName + ".enc";
        return uploadFile(USERS_PATH + login + "/" + fileName, keyBytes);
    }

    public boolean uploadUpdate(Update update, String login) throws Exception {
        String jsonData = update.toJson();
        String datetime = LocalDateTime.now().format(formatter);
        String fileName = login + "_update_" + datetime + ".json";
        return uploadFile(UPDATE_PATH + fileName, jsonData.getBytes(StandardCharsets.UTF_8));
    }

    public List<Pair<Pair<String, String>, Update>> downloadAllUpdates() throws Exception {
        return executorService.submit(() -> {
            List<Pair<Pair<String, String>, Update>> updatesList = new ArrayList<>();
            List<String> updateFiles = getFileNamesInFolder(UPDATE_PATH,
                    item -> item.getName().endsWith(".json") && item.getName().contains("_update_"));

            for (String fileName : updateFiles) {
                try {
                    Pair<String, String> loginDate = parseUpdateFileName(fileName);
                    if (loginDate != null) {
                        Update update = downloadAndParseJson(UPDATE_PATH + fileName, Update.class);
                        updatesList.add(new Pair<>(loginDate, update));
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при обработке файла: " + fileName + " - " + e.getMessage());
                }
            }
            return updatesList;
        }).get();
    }

    private Pair<String, String> parseUpdateFileName(String fileName) {
        String[] parts = fileName.split("_update_");
        if (parts.length == 2) {
            String login = parts[0];
            String datetime = parts[1].replace(".json", "");
            return new Pair<>(login, datetime);
        }
        return null;
    }

    public boolean uploadRegistrationRequest(RegistrationForm registrationForm) throws Exception {
        String jsonData = registrationForm.toJson();
        String fileName = registrationForm.getLogin() + ".json";
        return uploadFile(REGISTRATION_PATH + fileName, jsonData.getBytes(StandardCharsets.UTF_8));
    }

    public RegistrationForm downloadRegistrationForm(String login) {
        try {
            return downloadAndParseJson(REGISTRATION_PATH + login, RegistrationForm.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download registration form for login: " + login, e);
        }
    }

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

    public boolean deleteRegistrationRequest(String login) {
        return deleteFile(REGISTRATION_PATH + login + ".json");
    }

    public boolean deleteUpdateFile(String fileName) {
        return deleteFile(UPDATE_PATH + fileName);
    }

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
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ ====================

    @FunctionalInterface
    private interface HttpRequestProcessor<T> {
        T process(HttpURLConnection connection) throws Exception;
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