// UpdatesModule.java (обновленная асинхронная версия)
package com.pancreatitis.modules.updates;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.authorization.AuthorizationModule;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.safety.SafetyModule;
import javafx.util.Pair;

import javax.crypto.SecretKey;
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
                                    // Загружаем обновление
                                    Pair<Pair<String, String>, Update> updatePair =
                                            cloudStorageModule.downloadUpdateAsync(fileName).get();

                                    if (updatePair == null) {
                                        return new UpdateLoadResult(index, null, null, null, false,
                                                "Не удалось распарсить файл", fileName);
                                    }

                                    // Обрабатываем загруженное обновление
                                    return processUpdate(index, updatePair, fileName);

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

                // Шаг 3: Ждем завершения всех задач
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // Шаг 4: Подсчитываем результаты
                int successCount = 0;
                int failCount = 0;

                for (CompletableFuture<UpdateLoadResult> future : futures) {
                    UpdateLoadResult result = future.get();
                    if (result.isSuccess()) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                }

                // Шаг 5: Завершаем загрузку
                callback.onComplete(successCount, failCount);

            } catch (Exception ex) {
                callback.onError("Ошибка загрузки обновлений: " + ex.getMessage());
            }
        });
    }

    /**
     * Обработка одного обновления
     */
    private UpdateLoadResult processUpdate(int index, Pair<Pair<String, String>, Update> updatePair, String fileName) {
        try {
            Update update = updatePair.getValue();
            String doctor = updatePair.getKey().getKey();

            // Аутентификация врача для получения ключа
            SecretKey key_admin = authorizationModule.authenticateForAdmin(doctor);

            // 1. Обработка пациента
            Patient patient = update.getPatient();
            if (patient != null && patient.getFio() != null) {
                patient.setFio(safetyModule.decryptString(patient.getFio(), key_admin));
            }
            patient.setId(-1);
            patientList.add(patient);

            // 2. Обработка анкеты
            QuestionnaireDTO dto = update.getQuestionnaireDTO();
            Questionnaire questionnaire = new Questionnaire(dto);
            questionnaire.setId(-1);
            questionnairList.add(questionnaire);

            // 3. Получение характеристик
            List<CharacterizationAnketPatient> characteristics = dto.getCharacteristicValues();
            characterizationAnketPatientList.add(characteristics);

            return new UpdateLoadResult(index, patient, questionnaire, characteristics,
                    true, null, fileName);

        } catch (Exception ex) {
            return new UpdateLoadResult(index, null, null, null, false,
                    ex.getMessage(), fileName);
        }
    }

    /**
     * Синхронная загрузка (для обратной совместимости)
     */
    public void load() {
        try {
            updatesList = new ArrayList<>();
            questionnairList = new ArrayList<>();
            patientList = new ArrayList<>();
            characterizationAnketPatientList = new ArrayList<>();

            updatesList = cloudStorageModule.downloadAllUpdates();

            for (Pair<Pair<String, String>, Update> pair : updatesList) {
                Update update = pair.getValue();
                String doctor = pair.getKey().getKey();
                SecretKey key_admin = authorizationModule.authenticateForAdmin(doctor);

                Patient patient = update.getPatient();
                if (patient != null && patient.getFio() != null) {
                    patient.setFio(safetyModule.decryptString(patient.getFio(), key_admin));
                }
                patient.setId(-1);
                patientList.add(patient);

                QuestionnaireDTO dto = update.getQuestionnaireDTO();
                Questionnaire questionnaire = new Questionnaire(dto);
                questionnaire.setId(-1);
                questionnairList.add(questionnaire);

                List<CharacterizationAnketPatient> localCcharacterizationAnketPatientList = dto.getCharacteristicValues();
                characterizationAnketPatientList.add(localCcharacterizationAnketPatientList);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void clearTempData() {
        patientList.clear();
        questionnairList.clear();
        characterizationAnketPatientList.clear();
        updatesList.clear();
    }

    public void deleteUpdate(int id) {
        if (id >= 0 && id < updatesList.size()) {
            Pair<Pair<String, String>, Update> pair = updatesList.get(id);
            Update update = pair.getValue();
            String doctor = pair.getKey().getKey();
            String datetime = pair.getKey().getValue();
            cloudStorageModule.deleteUpdateFile(String.format("%s_update_%s.json", doctor, datetime));
            updatesList.remove(id);

            if (id < patientList.size()) {
                patientList.remove(id);
                questionnairList.remove(id);
                characterizationAnketPatientList.remove(id);
            }
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