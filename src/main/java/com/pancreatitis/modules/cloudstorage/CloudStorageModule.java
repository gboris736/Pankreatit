package com.pancreatitis.modules.cloudstorage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.models.Update;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.trainset.TrainingData;
import com.pancreatitis.modules.trainset.TrainingDataParser;
import javafx.util.Pair;

import okhttp3.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class CloudStorageModule {
    private final String authToken = "y0__xC7koqFBxjIhTwg6O2OwBUwreWe7AeTLwUn1hxDxrN6Pp9NW10aTKlPGw";
    private static final String API_URL = "https://cloud-api.yandex.net/v1/disk/resources";
    private static CloudStorageModule instance;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Константы для путей
    //private static final String USERS_PATH = "/users/";
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
                    .put(RequestBody.create(new byte[0], null)) // пустое тело
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

                // Добавлена недостающая логика обработки
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

    /**
     * Асинхронная загрузка списка файлов обновлений (без содержимого)
     */
    public CompletableFuture<List<String>> getUpdateFileNamesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getFileNamesInFolder(UPDATE_PATH,
                        item -> item.getName().endsWith(".json") && item.getName().contains("_update_"));
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }

    /**
     * Асинхронная загрузка одного обновления по имени файла
     */
    public CompletableFuture<Pair<Pair<String, String>, Update>> downloadUpdateAsync(String fileName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Pair<String, String> loginDate = parseUpdateFileName(fileName);
                if (loginDate != null) {
                    Update update = downloadAndParseJson(UPDATE_PATH + fileName, Update.class);
                    return new Pair<>(loginDate, update);
                }
                return null;
            } catch (Exception e) {
                throw new CompletionException("Ошибка при обработке файла: " + fileName, e);
            }
        }, executorService);
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