package com.pancreatitis.modules.trainset;

import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TrainSetModule {
    private static LocalStorageModule localStorageModule;
    private static CloudStorageModule cloudStorageModule;
    private static TrainSetModule instance;
    private TrainingData trainingData;

    private TrainSetModule() {
        localStorageModule = LocalStorageModule.getInstance();
        cloudStorageModule = CloudStorageModule.getInstance();
    }

    public static TrainSetModule getInstance() {
        if (instance == null) {
            synchronized (TrainSetModule.class) {
                if (instance == null) {
                    instance = new TrainSetModule();
                }
            }
        }
        return instance;
    }

    // это для поддержки старых версий, пока забыли про это
    private boolean load(String version) {
        try {
            byte[] rawTrainingData = localStorageModule.getTrainingData(version);

            TrainingData trainingData = TrainingDataParser.parseFromFile(
                    new ByteArrayInputStream(rawTrainingData)
            );

            this.trainingData = trainingData;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean load() {
        return load("algorithm.txt");
    }

    public boolean saveChanges() {
        try {
            String filePath = localStorageModule.getPathAlgorithmFile();
            String content = TrainingDataParser.serializeToTextFormat(trainingData);
            try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Integer> getQuestionIdTrainList() throws TrainingDataException{
        List<Integer> ret = new ArrayList<>();
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        for ( float[] record : trainingData.getTrainingRecords() ){
            int id = (int) record[0];
            if (databaseModule.containsQuestion(id)) {
                ret.add( id );
            }
            else {
                throw new TrainingDataException( String.format("Not found this questionnaire in data base, %d", id) );
            }
        }

        return ret;
    }

    public boolean submit() {
        try {
            return cloudStorageModule.uploadTrainingData(trainingData);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean addQuestionnaire(Questionnaire questionnaire, List<CharacterizationAnketPatient> characterizationAnketPatientLists) {
        try {
            int codeDiagnosis = Integer.parseInt(questionnaire.getDiagnosis());
            long idQuestionnaire = questionnaire.getId();
            float[] records = new float[characterizationAnketPatientLists.size() + 2];
            records[0] = idQuestionnaire;
            for(int i = 0; i < characterizationAnketPatientLists.size(); i++) {
                CharacterizationAnketPatient characterizationAnketPatient = characterizationAnketPatientLists.get(i);
                if (characterizationAnketPatient.getValue() != -1) {
                    records[i+1] = characterizationAnketPatient.getValue();
                } else {
                    records[i+1] = characterizationAnketPatient.getIdValue();
                }
            }
            records[characterizationAnketPatientLists.size() + 1] = codeDiagnosis;
            return trainingData.addRecord(records, codeDiagnosis);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteQuestionnaire(Questionnaire questionnaire) {
        try {
            long idQuestionnaire = questionnaire.getId();
            return trainingData.deleteRecord(idQuestionnaire);
        } catch (Exception e) {
            return false;
        }
    }
}
