package com.pancreatitis.pankreat2.modules.questionnairemanager;

import com.pancreatitis.pankreat2.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.pankreat2.modules.database.DatabaseModule;
import com.pancreatitis.pankreat2.modules.trainset.TrainSetModule;

public class QuestionnaireManagerModule {
    private static QuestionnaireManagerModule instance;
    private DatabaseModule databaseModule;
    private CloudStorageModule cloudStorageModule;
    private void QuestionnaireManagerModule() {
        databaseModule = DatabaseModule.getInstance();
        cloudStorageModule = CloudStorageModule.getInstance();
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
}
