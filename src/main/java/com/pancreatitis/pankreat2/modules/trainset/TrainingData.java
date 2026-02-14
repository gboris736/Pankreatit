package com.pancreatitis.pankreat2.modules.trainset;

import java.util.List;

public interface TrainingData {
    List<float[]> getTrainingRecords();

    String getVersion();

    int[] getCharacteristicIds();

    int getTrainingDataSize();

    int getCharacteristicsCount();
}
