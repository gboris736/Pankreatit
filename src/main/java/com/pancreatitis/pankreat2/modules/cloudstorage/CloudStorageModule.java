package com.pancreatitis.pankreat2.modules.cloudstorage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pancreatitis.pankreat2.models.Doctor;
import com.pancreatitis.pankreat2.models.RegistrationForm;
import com.pancreatitis.pankreat2.models.Update;
import com.pancreatitis.pankreat2.models.User;
import com.pancreatitis.pankreat2.modules.database.DatabaseModule;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CloudStorageModule {
    private final String authToken = "y0__xC7koqFBxjIhTwg6O2OwBUwreWe7AeTLwUn1hxDxrN6Pp9NW10aTKlPGw";
    private static final String API_URL = "https://cloud-api.yandex.net/v1/disk/resources";
    private static CloudStorageModule instance;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");

    private CloudStorageModule() {
        // Инициализация папок в фоне
        executorService.execute(() -> {
            createFolder("/users/");
            createFolder("/update/");
            createFolder("/registration_requests/");
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

    private boolean createFolder(String path) {
        try {
            String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
            String url = API_URL + "?path=" + encodedPath;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Authorization", "OAuth " + authToken);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            // 201 - created, 409 - already exists (acceptable)
            return responseCode == 201 || responseCode == 409;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean createFolder(String folder, String login) {
        try {
            String encodedPath = URLEncoder.encode(String.format("/%s/%s", folder, login), StandardCharsets.UTF_8.toString());
            String url = API_URL + "?path=" + encodedPath;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Authorization", "OAuth " + authToken);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            // 201 - created, 409 - already exists (acceptable)
            return responseCode == 201 || responseCode == 409;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isUserFolderExists(String login) {
        try {
            Future<Boolean> future = executorService.submit(() -> {
                String path = "/users/" + login;
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
                String url = API_URL + "?path=" + encodedPath;

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "OAuth " + authToken);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                connection.disconnect();

                // 200 - exists
                return responseCode == 200;
            });

            return future.get(); // Блокируем до получения результата
        } catch (Exception e) {
            return false;
        }
    }

    public byte[] downloadUserKey(String login) throws Exception {
        Callable<byte[]> task = () -> {
            String filePath = "/users/" + login + "/key_user.enc";
            String downloadUrl = getDownloadUrl(filePath);

            if (downloadUrl == null) {
                throw new FileNotFoundException("File not found: " + filePath);
            }

            return downloadFileAsBytes(downloadUrl);
        };

        return executorService.submit(task).get();
    }

    public boolean uploadUserKey(String login, byte[] keyBytes, String keyName) throws Exception {
        Callable<Boolean> task = () -> {
            String fileName = keyName + ".enc";
            String filePath = "/users/" + login + "/" + fileName;

            String uploadUrl = getUploadUrl(filePath, true);
            if (uploadUrl == null) {
                System.err.println("Не удалось получить URL для загрузки ключа пользователя: " + filePath);
                return false;
            }

            return uploadFile(uploadUrl, keyBytes);
        };

        return executorService.submit(task).get();
    }

    public boolean uploadUpdate(Update update, String login) throws Exception {
        Callable<Boolean> task = () -> {
            String jsonData = update.toJson();
            String datetime = LocalDateTime.now().format(formatter);
            String fileName = login + "_update_" + datetime + ".json";
            String filePath = "/update/" + fileName;

            String uploadUrl = getUploadUrl(filePath, true);
            if (uploadUrl == null) {
                System.err.println("Не удалось получить URL для загрузки файла: " + filePath);
                return false;
            }

            return uploadFile(uploadUrl, jsonData.getBytes(StandardCharsets.UTF_8));
        };

        return executorService.submit(task).get();
    }

    public List<Pair<Pair<String, String>, Update>> downloadAllUpdates() throws Exception {
        Callable<List<Pair<Pair<String, String>, Update>>> task = () -> {
            List<Pair<Pair<String, String>, Update>> updatesList = new ArrayList<>();

            // Получаем список файлов в папке update
            String path = "/update/";
            String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
            String url = API_URL + "?path=" + encodedPath;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "OAuth " + authToken);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            try {
                int responseCode = connection.getResponseCode();

                if (responseCode == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        String responseStr = response.toString();
                        int itemsIndex = responseStr.indexOf("\"items\":[");

                        if (itemsIndex != -1) {
                            String itemsPart = responseStr.substring(itemsIndex);
                            int nameIndex = 0;

                            while ((nameIndex = itemsPart.indexOf("\"name\":\"", nameIndex)) != -1) {
                                int start = nameIndex + 8;
                                int end = itemsPart.indexOf("\"", start);
                                String fileName = itemsPart.substring(start, end);

                                // Обрабатываем только файлы обновлений
                                if (fileName.endsWith(".json") && fileName.contains("_update_")) {
                                    processUpdateFile(fileName, updatesList);
                                }
                                nameIndex = end;
                            }
                        }
                    }
                }
                return updatesList;
            } finally {
                connection.disconnect();
            }
        };

        return executorService.submit(task).get();
    }

    private void processUpdateFile(String fileName, List<Pair<Pair<String, String>, Update>> updatesList) {
        try {
            // Извлекаем логин и дату из имени файла
            String[] parts = fileName.split("_update_");
            if (parts.length == 2) {
                String login = parts[0];
                String datetime = parts[1].replace(".json", "");

                // Скачиваем файл обновления
                String filePath = "/update/" + fileName;
                String downloadUrl = getDownloadUrl(filePath);

                if (downloadUrl != null) {
                    byte[] jsonBytes = downloadFileAsBytes(downloadUrl);
                    String json = new String(jsonBytes, StandardCharsets.UTF_8);

                    // Парсим JSON в объект Update
                    ObjectMapper mapper = new ObjectMapper();
                    Update update = mapper.readValue(json, Update.class);

                    // Создаем структуру данных
                    Pair<String, String> loginDateTimePair = new Pair<>(login, datetime);
                    Pair<Pair<String, String>, Update> resultPair = new Pair<>(loginDateTimePair, update);

                    updatesList.add(resultPair);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обработке файла: " + fileName + " - " + e.getMessage());
        }
    }

    public boolean uploadRegistrationRequest(RegistrationForm registrationForm) throws Exception {
        Callable<Boolean> task = () -> {
            String jsonData = registrationForm.toJson();

            String fileName = registrationForm.getLogin() + ".json";
            String filePath = "/registration_requests/" + fileName;

            String uploadUrl = getUploadUrl(filePath, true);
            if (uploadUrl == null) {
                System.err.println("Не удалось получить URL для загрузки файла: " + filePath);
                return false;
            }

            return uploadFile(uploadUrl, jsonData.getBytes(StandardCharsets.UTF_8));
        };

        return executorService.submit(task).get();
    }

    public User downloadUserInfo(String login) {
        try {
            Callable<User> task = () -> {
                String filePath = "/users/" + login + "/user_info.json";
                String downloadUrl = getDownloadUrl(filePath);

                if (downloadUrl == null) {
                    throw new FileNotFoundException("User info not found: " + filePath);
                }

                // Скачиваем JSON файл
                byte[] jsonBytes = downloadFileAsBytes(downloadUrl);
                String json = new String(jsonBytes, StandardCharsets.UTF_8);

                // Парсим JSON в объект User
                return parseUserFromJson(json, login);
            };

            return executorService.submit(task).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean uploadUserInfo(RegistrationForm registrationForm) throws Exception {
        Callable<Boolean> task = () -> {
            String login = registrationForm.getLogin();
            if (login == null || login.isEmpty()) {
                System.err.println("Логин пользователя не указан");
                return false;
            }

            String jsonData = registrationForm.toJson();

            String fileName = "user_info.json";
            String filePath = "/users/" + login + "/" + fileName;

            String uploadUrl = getUploadUrl(filePath, true);
            if (uploadUrl == null) {
                System.err.println("Не удалось получить URL для загрузки информации о пользователе: " + filePath);
                return false;
            }

            return uploadFile(uploadUrl, jsonData.getBytes(StandardCharsets.UTF_8));
        };

        return executorService.submit(task).get();
    }

    // Вспомогательный метод для парсинга JSON
    private User parseUserFromJson(String json, String login) throws Exception {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);

            // Получаем singleton-объект User
            User user = User.getInstance();

            // Заполняем поля из JSON
            if (rootNode.has("email")) {
                user.setEmail(rootNode.get("email").asText());
            }

            if (rootNode.has("fullName")) {
                // Если у вас есть объект Doctor
                Doctor doctor = new Doctor();
                doctor.setFio(rootNode.get("fullName").asText());
                user.setDoctor(doctor);
            }

            if (rootNode.has("phone")) {
                user.setPhone(rootNode.get("phone").asText());
            }

            // Логин передаем параметром, так как он в пути файла
            user.setLogin(login);

            return user;
        } catch (Exception e) {
            throw new IOException("Failed to parse user JSON: " + e.getMessage(), e);
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        List<String> logins = getAllUserLogins();
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        for(String login: logins){
            User user = downloadUserInfo(login);
            Doctor doctor = databaseModule.getDoctorByFio(user.getFullName());
            user.setDoctor(doctor);
            users.add(user);
        }
        return users;
    }

    private List<String> getAllUserLogins() {
        try {
            Callable<List<String>> task = () -> {
                String path = "/users/";
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
                String url = API_URL + "?path=" + encodedPath;

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "OAuth " + authToken);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                try {
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(connection.getInputStream());

                        List<String> userLogins = new ArrayList<>();
                        JsonNode itemsNode = rootNode.get("_embedded").get("items");

                        if (itemsNode != null && itemsNode.isArray()) {
                            for (JsonNode item : itemsNode) {
                                String name = item.get("name").asText();
                                String type = item.get("type").asText();

                                // Добавляем только папки (на всякий случай проверяем тип)
                                if ("dir".equals(type)) {
                                    userLogins.add(name);
                                }
                            }
                        }

                        return userLogins;
                    }
                    return new ArrayList<>();
                } finally {
                    connection.disconnect();
                }
            };

            return executorService.submit(task).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] downloadTrainingData() throws Exception {
        Callable<byte[]> task = () -> {
            String filePath = "/algorithm.txt";
            String downloadUrl = getDownloadUrl(filePath);

            if (downloadUrl == null) {
                throw new FileNotFoundException("File not found: " + filePath);
            }

            return downloadFileAsBytes(downloadUrl);
        };

        return executorService.submit(task).get();
    }

    // Вспомогательные методы (остаются теми же)
    private byte[] downloadFileAsBytes(String downloadUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (InputStream inputStream = connection.getInputStream();
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    return outputStream.toByteArray();
                }
            } else {
                throw new IOException("Failed to download file. Response code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    private String getDownloadUrl(String path) throws Exception {
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
        String url = API_URL + "/download?path=" + encodedPath;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "OAuth " + authToken);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Парсим JSON для получения ссылки
                    String responseStr = response.toString();
                    int hrefIndex = responseStr.indexOf("\"href\":\"");
                    if (hrefIndex != -1) {
                        int start = hrefIndex + 8;
                        int end = responseStr.indexOf("\"", start);
                        return responseStr.substring(start, end);
                    }
                }
            }
            return null;
        } finally {
            connection.disconnect();
        }
    }

    private String getUploadUrl(String path, boolean overwrite) throws Exception {
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
        String url = API_URL + "/upload?path=" + encodedPath + "&overwrite=" + overwrite;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "OAuth " + authToken);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    String responseStr = response.toString();
                    int hrefIndex = responseStr.indexOf("\"href\":\"");
                    if (hrefIndex != -1) {
                        int start = hrefIndex + 8;
                        int end = responseStr.indexOf("\"", start);
                        return responseStr.substring(start, end);
                    }
                }
            }
            return null;
        } finally {
            connection.disconnect();
        }
    }

    private boolean uploadFile(String uploadUrl, byte[] data) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(data);
            outputStream.flush();
        }

        int responseCode = connection.getResponseCode();
        connection.disconnect();

        return responseCode == 201 || responseCode == 200;
    }

    // Метод для очистки ресурсов
    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}