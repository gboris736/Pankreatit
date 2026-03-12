package com.pancreatitis.modules.updates;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.authorization.AuthorizationModule;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.safety.SafetyModule;
import javafx.util.Pair;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

public class UpdatesModule {
    private static CloudStorageModule cloudStorageModule = CloudStorageModule.getInstance();
    private static AuthorizationModule authorizationModule = AuthorizationModule.getInstance();
    private static SafetyModule safetyModule = SafetyModule.getInstance();
    private List<Patient> patientList = new ArrayList<>();
    private List<Questionnaire> questionnairList = new ArrayList<>();
    private List<List<CharacterizationAnketPatient>> characterizationAnketPatientList = new ArrayList<>();
    List<Pair<Pair<String, String>, Update>> updatesList = new ArrayList<>();
    private static UpdatesModule instance;

    private UpdatesModule() {

    }

    public static UpdatesModule getInstance(){
        if (instance == null) {
            instance = new UpdatesModule();
        }
        return instance;
    }

    public void load(){
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

                // 1. Обработка пациентов
                Patient patient = update.getPatient();
                patient.setFio(safetyModule.decryptString(patient.getFio(), key_admin));
                patient.setId(-1);
                patientList.add(patient);

                // 2. Обработка анкет и их характеристик
                QuestionnaireDTO dto = update.getQuestionnaireDTO();
                long questionnaireId = dto.getId();

                // Преобразование DTO в обычную анкету (копирование полей)
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

    public void deleteUpdate(int id) {
        Pair<Pair<String, String>, Update> pair = updatesList.get(id);
        Update update = pair.getValue();
        String doctor = pair.getKey().getKey();
        String datetime = pair.getKey().getValue();
        cloudStorageModule.deleteUpdateFile(String.format("%s_update_%s.json", doctor, datetime));
        updatesList.remove(id);
    }

    public List<Patient> getPatientList(){
        return patientList;
    }

    public List<Questionnaire> getQuestionnairList(){
        return questionnairList;
    }

    public List<List<CharacterizationAnketPatient>> getCharacterizationAnketPatientList(){
        return characterizationAnketPatientList;
    }
}
