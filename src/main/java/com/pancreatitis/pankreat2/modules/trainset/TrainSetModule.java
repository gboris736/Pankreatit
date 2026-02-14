package com.pancreatitis.pankreat2.modules.trainset;

import com.pancreatitis.pankreat2.models.Questionnaire;
import com.pancreatitis.pankreat2.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.pankreat2.modules.localstorage.LocalStorageModule;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class TrainSetModule {
    private LocalStorageModule localStorageModule;
    private CloudStorageModule cloudStorageModule;
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
            String filePath = ""; // через локальное хранилище получить путь, по идее константа
            String content = TrainingDataParser.serializeToTextFormat(trainingData);
            try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean submit() {
        try {
            return cloudStorageModule.uploadTrainingData(trainingData);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean addQuestionnaire(Questionnaire questionnaire) {
        try {


            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteQuestionnaire(Questionnaire questionnaire) {
        try {


            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
