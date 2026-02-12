// TextFileTrainingData.java
package com.pancreatitis.pankreat2.modules.prediction;

import java.util.*;

public class TextFileTrainingData implements TrainingData {

    private final String version;
    private final int[] characteristicIds;
    private final List<float[]> trainingRecords;
    private final int[] diagnoses; // Кэшированные диагнозы для быстрого доступа

    public TextFileTrainingData(String version, int[] characteristicIds,
                                List<float[]> trainingRecords) {
        this.version = version;
        this.characteristicIds = characteristicIds.clone();
        this.trainingRecords = Collections.unmodifiableList(trainingRecords);

        // Инициализируем массив диагнозов
        this.diagnoses = new int[trainingRecords.size()];
        for (int i = 0; i < trainingRecords.size(); i++) {
            float[] record = trainingRecords.get(i);
            this.diagnoses[i] = (int) record[record.length - 1]; // Последний элемент - диагноз
        }
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public List<float[]> getTrainingRecords() {
        return trainingRecords;
    }

    @Override
    public int[] getCharacteristicIds() {
        return characteristicIds.clone();
    }

    @Override
    public int getTrainingDataSize() {
        return trainingRecords.size();
    }

    @Override
    public int getCharacteristicsCount() {
        return characteristicIds.length;
    }

    /**
     * Возвращает только векторы признаков (без диагнозов)
     */
    public List<float[]> getFeatureVectors() {
        List<float[]> features = new ArrayList<>(trainingRecords.size());

        for (float[] record : trainingRecords) {
            // Копируем все элементы кроме последнего (диагноза)
            float[] featureVector = new float[record.length - 1];
            System.arraycopy(record, 0, featureVector, 0, featureVector.length);
            features.add(featureVector);
        }

        return Collections.unmodifiableList(features);
    }

    /**
     * Возвращает массив диагнозов для каждой записи
     */
    public int[] getDiagnoses() {
        return diagnoses.clone();
    }

    /**
     * Возвращает статистику по диагнозам
     */
    public Map<Integer, Integer> getDiagnosisStatistics() {
        Map<Integer, Integer> stats = new HashMap<>();

        for (int diagnosis : diagnoses) {
            stats.put(diagnosis, stats.getOrDefault(diagnosis, 0) + 1);
        }

        return Collections.unmodifiableMap(stats);
    }

    /**
     * Возвращает записи с определенным диагнозом
     */
    public List<float[]> getRecordsByDiagnosis(int diagnosis) {
        List<float[]> filtered = new ArrayList<>();

        for (float[] record : trainingRecords) {
            if ((int) record[record.length - 1] == diagnosis) {
                filtered.add(record);
            }
        }

        return Collections.unmodifiableList(filtered);
    }

    /**
     * Возвращает минимальное и максимальное значения для каждой характеристики
     */
    public Map<Integer, float[]> getCharacteristicRanges() {
        if (trainingRecords.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, float[]> ranges = new HashMap<>();
        int numCharacteristics = characteristicIds.length;

        // Инициализируем мин/макс значения
        for (int i = 0; i < numCharacteristics; i++) {
            ranges.put(characteristicIds[i], new float[]{
                    Float.MAX_VALUE, Float.MIN_VALUE
            });
        }

        // Проходим по всем записям и обновляем мин/макс
        for (float[] record : trainingRecords) {
            for (int i = 0; i < numCharacteristics; i++) {
                float value = record[i];
                float[] range = ranges.get(characteristicIds[i]);

                if (value < range[0]) range[0] = value;
                if (value > range[1]) range[1] = value;
            }
        }

        return Collections.unmodifiableMap(ranges);
    }

    @Override
    public String toString() {
        return String.format(
                "TrainingData{version=%s, characteristics=%d, records=%d}",
                version, characteristicIds.length, trainingRecords.size()
        );
    }
}