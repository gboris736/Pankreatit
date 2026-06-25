package com.pancreatitis.modules.questionnairemanager;

import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.User;
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

    public boolean saveQuestionnaire(Questionnaire questionnaire, Patient patient, List<CharacterizationAnketPatient> characterizationAnketPatients) {
        try {
            databaseModule.beginTransaction();

            questionnaire.setIdExpert(User.getInstance().getId());
            if (questionnaire.getIdDoctor() == -1) questionnaire.setIdDoctor(User.getInstance().getId());

            long idPatient = questionnaire.getIdPatient();
            if (idPatient == -1) {
                idPatient = databaseModule.insertPatient(patient);
                questionnaire.setIdPatient(idPatient);
                patient.setId(idPatient);
            } else {
                Patient patient_db = databaseModule.getPatientByFio(patient.getFio());
                if (patient_db == null) {
                    idPatient = databaseModule.insertPatient(patient);
                    questionnaire.setIdPatient(idPatient);
                    patient.setId(idPatient);
                } else {
                    idPatient = patient_db.getId();
                    questionnaire.setIdPatient(idPatient);
                    patient.setId(idPatient);
                }
            }

            long idQuestionnaire = questionnaire.getId();
            if (idQuestionnaire == -1) {
                idQuestionnaire = databaseModule.insertQuestionnaire(questionnaire);
                questionnaire.setId(idQuestionnaire);
            } else {
                databaseModule.updateQuestionnaire(questionnaire);
            }

            for(CharacterizationAnketPatient ch: characterizationAnketPatients){
                ch.setIdAnket(idQuestionnaire);
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