package com.pancreatitis.pankreat2.modules.prediction;

import java.util.List;

interface TrainingData {
    List<float[]> getTrainingRecords();

    String getVersion();

    int[] getCharacteristicIds();

    int getTrainingDataSize();

    int getCharacteristicsCount();
}
