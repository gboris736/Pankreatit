// UpdatesModule.java (модифицированная версия)
package com.pancreatitis.modules.updates;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.authorization.AuthorizationModule;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.safety.SafetyModule;
import javafx.util.Pair;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class UpdatesModule {
    private static CloudStorageModule cloudStorageModule = CloudStorageModule.getInstance();
    private static AuthorizationModule authorizationModule = AuthorizationModule.getInstance();
    private static SafetyModule safetyModule = SafetyModule.getInstance();

    private List<Patient> patientList = new ArrayList<>();
    private List<Questionnaire> questionnairList = new ArrayList<>();
    private List<List<CharacterizationAnketPatient>> characterizationAnketPatientList = new ArrayList<>();
    private List<Pair<Pair<String, String>, Update>> updatesList = new ArrayList<>();
    private static UpdatesModule instance;

    // Для обратной совместимости - синхронная загрузка
    private boolean asyncMode = false;

    private UpdatesModule() {

    }

    public static UpdatesModule getInstance() {
        if (instance == null) {
            instance = new UpdatesModule();
        }
        return instance;
    }

    // Асинхронная загрузка с callback
    public void loadAsync(UpdateLoadCallback callback) {
        asyncMode = true;

        // Запускаем загрузку в отдельном потоке
        new Thread(() -> {
            try {
                // Получаем список обновлений
                List<Pair<Pair<String, String>, Update>> updates = cloudStorageModule.downloadAllUpdates();
                int total = updates.size();

                if (total == 0) {
                    callback.onStart(0);
                    callback.onComplete(0, 0);
                    return;
                }

                // Уведомляем о начале загрузки
                callback.onStart(total);

                // Создаем пул потоков для параллельной обработки (3 потока)
                ExecutorService executor = Executors.newFixedThreadPool(3);
                CompletionService<UpdateLoadResult> completionService = new ExecutorCompletionService<>(executor);

                // Отправляем задачи на обработку
                for (int i = 0; i < total; i++) {
                    completionService.submit(new UpdateProcessingTask(i, updates.get(i)));
                }

                // Собираем результаты
                int loaded = 0;
                int successCount = 0;
                int failCount = 0;

                for (int i = 0; i < total; i++) {
                    try {
                        UpdateLoadResult result = completionService.take().get();
                        loaded++;

                        // Уведомляем о прогрессе
                        callback.onProgress(loaded, total);

                        // Уведомляем о загруженном обновлении
                        callback.onUpdateLoaded(result);

                        if (result.isSuccess()) {
                            successCount++;
                        } else {
                            failCount++;
                        }

                    } catch (Exception e) {
                        failCount++;
                        callback.onError("Ошибка при обработке обновления: " + e.getMessage());
                    }
                }

                // Завершаем работу пула
                executor.shutdown();

                // Уведомляем о завершении
                callback.onComplete(successCount, failCount);

                // Очищаем временные списки после асинхронной загрузки
                if (asyncMode) {
                    clearTempData();
                }

            } catch (Exception ex) {
                callback.onError("Ошибка загрузки обновлений: " + ex.getMessage());
            }
        }).start();
    }

    // Синхронная загрузка (для обратной совместимости)
    public void load() {
        asyncMode = false;
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
                patient.setFio(safetyModule.decryptString(patient.getFio(), key_admin));
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

            // Также удаляем из временных списков
            if (!asyncMode && id < patientList.size()) {
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

    public boolean isAsyncMode() {
        return asyncMode;
    }
}