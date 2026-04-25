// UpdatesModule.java (обновленная асинхронная версия)
package com.pancreatitis.modules.updates;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.authorization.AuthorizationModule;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.safety.SafetyModule;
import javafx.util.Pair;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UpdatesModule {
    private static CloudStorageModule cloudStorageModule = CloudStorageModule.getInstance();
    private static AuthorizationModule authorizationModule = AuthorizationModule.getInstance();
    private static SafetyModule safetyModule = SafetyModule.getInstance();

    private List<Patient> patientList = new ArrayList<>();
    private List<Questionnaire> questionnairList = new ArrayList<>();
    private List<List<CharacterizationAnketPatient>> characterizationAnketPatientList = new ArrayList<>();
    private List<Pair<Pair<String, String>, Update>> updatesList = new ArrayList<>();
    private List<String> updateUuids = new ArrayList<>();
    private static UpdatesModule instance;

    private ExecutorService processingExecutor;

    private UpdatesModule() {
        // Создаем пул для обработки с 3 потоками
        processingExecutor = Executors.newFixedThreadPool(3);
    }

    public static UpdatesModule getInstance() {
        if (instance == null) {
            instance = new UpdatesModule();
        }
        return instance;
    }

    /**
     * Асинхронная загрузка с callback
     */
    public void loadAsync(UpdateLoadCallback callback) {
        // Запускаем загрузку в отдельном потоке
        CompletableFuture.runAsync(() -> {
            try {
                // Шаг 1: Получаем список файлов обновлений
                List<String> fileNames = cloudStorageModule.getUpdateFileNamesAsync().get();
                int total = fileNames.size();

                if (total == 0) {
                    callback.onStart(0);
                    callback.onComplete(0, 0);
                    return;
                }

                // Уведомляем о начале загрузки
                callback.onStart(total);

                // Шаг 2: Создаем задачи для загрузки и обработки каждого файла
                List<CompletableFuture<UpdateLoadResult>> futures = new ArrayList<>();
                AtomicInteger loadedCount = new AtomicInteger(0);

                for (int i = 0; i < total; i++) {
                    final int index = i;
                    final String fileName = fileNames.get(i);

                    CompletableFuture<UpdateLoadResult> future = CompletableFuture
                            .supplyAsync(() -> {
                                try {
                                    // Извлекаем логин из имени файла (до первого '_')
                                    String login = extractLoginFromFileName(fileName);
                                    Update update = cloudStorageModule.downloadEncryptedUpdate(fileName, login);
                                    return processUpdate(index, fileName, update);
                                } catch (Exception e) {
                                    return new UpdateLoadResult(index, null, null, null, false,
                                            e.getMessage(), fileName);
                                }
                            }, processingExecutor)
                            .thenApply(result -> {
                                // Обновляем прогресс
                                int loaded = loadedCount.incrementAndGet();
                                callback.onProgress(loaded, total);
                                callback.onUpdateLoaded(result);
                                return result;
                            });

                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                int successCount = 0, failCount = 0;
                for (CompletableFuture<UpdateLoadResult> future : futures) {
                    if (future.get().isSuccess()) successCount++;
                    else failCount++;
                }
                callback.onComplete(successCount, failCount);

            } catch (Exception ex) {
                callback.onError("Ошибка загрузки обновлений: " + ex.getMessage());
            }
        });
    }

    private String encodeToSha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Извлекает логин из имени файла формата login_uuid.json
     */
    private String extractLoginFromFileName(String fileName) {
        int underscoreIndex = fileName.lastIndexOf('_');
        if (underscoreIndex > 0) {
            return fileName.substring(0, underscoreIndex);
        }
        throw new IllegalArgumentException("Invalid file name format: " + fileName);
    }

    /**
     * Извлекает uuid из имени файла формата login_uuid.json
     */
    private String extractUUIDFromFileName(String fileName) {
        int lastUnderscoreIndex = fileName.lastIndexOf('_');
        int dotIndex = fileName.lastIndexOf('.');
        if (lastUnderscoreIndex > 0 && dotIndex > lastUnderscoreIndex) {
            return fileName.substring(lastUnderscoreIndex + 1, dotIndex);
        }
        throw new IllegalArgumentException("Invalid file name format: " + fileName);
    }

    /**
     * Обработка одного обновления
     */
    private UpdateLoadResult processUpdate(int index, String fileName, Update update) {
        try {
            String login = update.getLogin();
            String dateUpload = update.getDateUpload();
            String uuid = extractUUIDFromFileName(fileName);
            Pair<String, String> loginDatePair = new Pair<>(login, dateUpload);
            Pair<Pair<String, String>, Update> entry = new Pair<>(loginDatePair, update);

            synchronized (this) {
                while (updatesList.size() <= index) {
                    updatesList.add(null);
                    patientList.add(null);
                    updateUuids.add(null);
                    questionnairList.add(null);
                    characterizationAnketPatientList.add(null);
                }
                updatesList.set(index, entry);
                updateUuids.set(index, uuid);
            }

            Patient patient = update.getPatient();
            patient.setId(-1);
            patientList.set(index, patient);

            QuestionnaireDTO dto = update.getQuestionnaireDTO();
            Questionnaire questionnaire = new Questionnaire(dto);
            questionnaire.setId(-1);
            questionnaire.setIdPatient(-1);
            questionnairList.set(index, questionnaire);

            List<CharacterizationAnketPatient> characteristics = dto.getCharacteristicValues();
            characterizationAnketPatientList.set(index, characteristics);

            return new UpdateLoadResult(index, patient, questionnaire, characteristics,
                    true, null, fileName);
        } catch (Exception ex) {
            return new UpdateLoadResult(index, null, null, null, false,
                    ex.getMessage(), fileName);
        }
    }

    private void clearTempData() {
        patientList.clear();
        questionnairList.clear();
        characterizationAnketPatientList.clear();
        updatesList.clear();
    }

    /**
     * Удаление обновления по индексу (теперь используется UUID)
     */
    public void deleteUpdate(int id) {
        if (id >= 0 && id < updatesList.size()) {
            Pair<Pair<String, String>, Update> pair = updatesList.get(id);
            String doctor = pair.getKey().getKey();
            String uuid = updateUuids.get(id);
            cloudStorageModule.deleteUpdateFile(String.format("%s_%s.json", doctor, uuid));

            updatesList.remove(id);
            updateUuids.remove(id);
            patientList.remove(id);
            questionnairList.remove(id);
            characterizationAnketPatientList.remove(id);
        }
    }

    public List<Patient> getPatientList() {
        return patientList;
    }

    public List<Questionnaire> getQuestionnairList() {
        return questionnairList;
    }

    public List<List<CharacterizationAnketPatient>> getCharacterizationAnketPatientList() {
        return characterizationAnketPatientList;
    }
}