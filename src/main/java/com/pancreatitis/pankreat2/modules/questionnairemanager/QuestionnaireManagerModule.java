package com.pancreatitis.pankreat2.modules.questionnairemanager;

import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.CharasteristicDTO;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.QuestionnaireDTO;
import com.pancreatitis.models.Update;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.safety.SafetyModule;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

public class QuestionnaireManagerModule {
    private static DatabaseModule databaseModule;
    private static SafetyModule safetyModule;
    private static CloudStorageModule cloudStorageModule;
    private static QuestionnaireManagerModule instance;
    private QuestionnaireManagerModule(){
        cloudStorageModule = CloudStorageModule.getInstance();
        safetyModule = SafetyModule.getInstance();
        databaseModule = DatabaseModule.getInstance();
    }
    public static QuestionnaireManagerModule getInstance(){
        if (instance == null) {
            instance = new QuestionnaireManagerModule();
        }
        return instance;
    }
    public String saveQuestionnaire(Questionnaire questionnaire, Patient patient){
        try {
            databaseModule.beginTransaction();

            long idPatient = questionnaire.getIdPatient();
            if (idPatient == -1){
                idPatient = databaseModule.insertPatient(patient);
                questionnaire.setIdPatient(idPatient);
                patient.setId(idPatient);
            }

            long idQuestionnaire = questionnaire.getId();
            if (idQuestionnaire == -1){
                idQuestionnaire = databaseModule.insertQuestionnaire(questionnaire);
                questionnaire.setId(idQuestionnaire);
            } else {
                databaseModule.updateQuestionnaire(questionnaire);
            }

            databaseModule.setTransactionSuccessful();

            return "Успешно";
        } catch (Exception e){
            return e.toString();
        } finally {
            databaseModule.endTransaction();
        }
    }
    public String submitQuestionnaire(List<Questionnaire> questionnaires, List<Patient> patients) {
        try {
            User user = User.getInstance();
            Update update = new Update();
            for (Patient patient: patients){
                encryptPatient(patient);
            }
            List<QuestionnaireDTO> questionnaireDTOS = buildQuestionnaireDTO(questionnaires);
            update.setPatientList(patients);
            update.setQuestionnaireDTOS(questionnaireDTOS);
            boolean result = cloudStorageModule.uploadQuestionnaire(update, user.getLogin());
            return result ? "Успешно" : "Ошибка";
        } catch (Exception e){
            return e.getMessage();
        }
    }

    private void encryptPatient(Patient patient) throws Exception {
        SecretKey key = User.getInstance().getKey();
        String encyprtFio = safetyModule.encryptString(patient.getFio(), key);
        patient.setFio(encyprtFio);
    }

    private List<QuestionnaireDTO> buildQuestionnaireDTO(List<Questionnaire> questionnaires){
        List<QuestionnaireDTO> questionnaireDTOS = new ArrayList<>();
        for (Questionnaire questionnaire: questionnaires){
            QuestionnaireDTO questionnaireDTO = new QuestionnaireDTO(questionnaire);
            List<CharasteristicDTO> charasteristicDTOS = buildCharacterusticDTO(questionnaire.getId());
            questionnaireDTO.setCharacteristicValues(charasteristicDTOS);
            questionnaireDTOS.add(questionnaireDTO);
        }
        return questionnaireDTOS;
    }

    private List<CharasteristicDTO> buildCharacterusticDTO(long idQuestionnaire){
        List<CharasteristicDTO> charasteristicDTOS = new ArrayList<>();
        List<Characteristic> charasteristics = databaseModule.getAllCharacteristics();

        List<CharacterizationAnketPatient> characterizationAnketPatients = databaseModule.getCharacterizationsForAnket(idQuestionnaire);
        for(CharacterizationAnketPatient characterizationAnketPatient: characterizationAnketPatients){
            Characteristic characteristic = charasteristics.get(characterizationAnketPatient.getIdCharacteristic() - 1);
            CharasteristicDTO charasteristicDTO = new CharasteristicDTO(characteristic);
            charasteristicDTO.setValue(characterizationAnketPatient.getIdValue());
            charasteristicDTOS.add(charasteristicDTO);
        }
        return charasteristicDTOS;
    }
}
