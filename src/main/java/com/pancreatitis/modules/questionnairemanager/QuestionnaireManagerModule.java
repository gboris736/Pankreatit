package com.pancreatitis.modules.questionnairemanager;

import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.modules.database.DatabaseModule;

import java.util.List;

public class QuestionnaireManagerModule {
    private static QuestionnaireManagerModule instance;
    private static DatabaseModule databaseModule;

    private QuestionnaireManagerModule() {
        databaseModule = DatabaseModule.getInstance();
    }

    public static QuestionnaireManagerModule getInstance() {
        if (instance == null) {
            synchronized (QuestionnaireManagerModule.class) {
                if (instance == null) {
                    instance = new QuestionnaireManagerModule();
                }
            }
        }
        return instance;
    }

    // переделать
    public boolean saveQuestionnaire(Questionnaire questionnaire, Patient patient, List<CharacterizationAnketPatient> characterizationAnketPatients) {
        try {
            databaseModule.beginTransaction();

            int idPatient = questionnaire.getIdPatient();
            if (idPatient == -1) {
                idPatient = databaseModule.insertPatient(patient);
                questionnaire.setIdPatient(idPatient);
                patient.setId(idPatient);
            }

            int idQuestionnaire = questionnaire.getId();
            if (idQuestionnaire == -1) {
                idQuestionnaire = databaseModule.insertQuestionnaire(questionnaire);
                questionnaire.setId(idQuestionnaire);
            } else {
                databaseModule.updateQuestionnaire(questionnaire);
            }

            databaseModule.insertAllCharacterizationAnketPatient(characterizationAnketPatients);

            databaseModule.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            databaseModule.endTransaction();
        }
    }
}