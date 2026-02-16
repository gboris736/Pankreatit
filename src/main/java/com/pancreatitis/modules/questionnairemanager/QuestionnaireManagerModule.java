package com.pancreatitis.modules.questionnairemanager;

import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.modules.database.DatabaseModule;

public class QuestionnaireManagerModule {
    private static QuestionnaireManagerModule instance;
    private static DatabaseModule databaseModule;

    private void QuestionnaireManagerModule() {
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
    private boolean saveQuestionnaire(Questionnaire questionnaire, Patient patient) {
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
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            databaseModule.endTransaction();
        }
    }

    // переделать
    private boolean saveValueCharacteristic(CharacterizationAnketPatient characterizationAnketPatient) {
        try {
            databaseModule.beginTransaction();

            databaseModule.insertCharacterizationAnketPatient(characterizationAnketPatient);

            databaseModule.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            databaseModule.endTransaction();
        }
    }
}
