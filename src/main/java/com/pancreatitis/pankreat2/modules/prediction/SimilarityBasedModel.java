// SimilarityBasedModel.java
package com.pancreatitis.pankreat2.modules.prediction;

import com.pancreatitis.pankreat2.models.Characteristic;
import java.util.*;

public class SimilarityBasedModel {

    private static class CharacteristicInfo {
        int id;
        int type;
        int weight;
        float minValue;
        float maxValue;
        boolean availableInPatient; // есть ли у пациента эта характеристика
    }

    private static class TrainingRecord {
        int diagnosisCode;
        float[] values;
    }

    private final Map<Integer, CharacteristicInfo> characteristicsMap; // по id
    private final List<CharacteristicInfo> characteristicsList; // в порядке тренировочных данных
    private final List<TrainingRecord> trainingRecords;
    private final Map<Integer, Integer> validDiagnoses = Map.of(1, 0, 5, 1, 6, 2);

    public SimilarityBasedModel(TrainingData trainingData, List<Characteristic> patientCharacteristics) {
        // 1. Получаем id характеристик из тренировочных данных
        int[] trainingCharIds = trainingData.getCharacteristicIds();

        // 2. Создаем мапу характеристик пациента по id
        Map<Integer, Characteristic> patientCharMap = new HashMap<>();
        for (Characteristic ch : patientCharacteristics) {
            patientCharMap.put(ch.getId(), ch);
        }

        // 3. Строим списки характеристик для модели
        this.characteristicsMap = new HashMap<>();
        this.characteristicsList = new ArrayList<>();

        for (int i = 0; i < trainingCharIds.length; i++) {
            int charId = trainingCharIds[i];

            CharacteristicInfo info = new CharacteristicInfo();
            info.id = charId;

            // Проверяем, есть ли эта характеристика у пациента
            Characteristic patientChar = patientCharMap.get(charId);
            if (patientChar != null) {
                // Характеристика есть у пациента
                info.type = patientChar.getIdType();
                info.weight = Math.round(patientChar.getWeight());
                info.minValue = patientChar.getMinValue();
                info.maxValue = patientChar.getMaxValue();
                info.availableInPatient = true;
            } else {
                // Характеристики нет у пациента
                info.type = 0;
                info.weight = 0;
                info.minValue = 0;
                info.maxValue = 0;
                info.availableInPatient = false;
            }

            characteristicsMap.put(charId, info);
            characteristicsList.add(info);
        }

        // 4. Преобразуем TrainingData в TrainingRecord
        this.trainingRecords = parseTrainingData(trainingData);
    }

    public PredictionResult predict(float[] features) {
        if (features.length != characteristicsList.size()) {
            throw new IllegalArgumentException(
                    "Неверное количество признаков. Ожидается: " +
                            characteristicsList.size() + ", получено: " + features.length
            );
        }

        // Инициализация статистики по классам
        ClassStat[] classes = new ClassStat[3];
        for (int i = 0; i < 3; i++) {
            classes[i] = new ClassStat();
        }

        // Сравнение с каждой обучающей записью
        for (TrainingRecord record : trainingRecords) {
            double similarity = calculateSimilarity(features, record.values);

            Integer classIndex = validDiagnoses.get(record.diagnosisCode);
            if (classIndex != null) {
                classes[classIndex].count++;
                classes[classIndex].totalSimilarity += similarity;
            }
        }

        // Рассчитываем вероятности
        Map<Integer, Float> probabilities = calculateProbabilities(classes);
        return new PredictionResult(probabilities);
    }

    private double calculateSimilarity(float[] patientValues, float[] trainingValues) {
        double similarity = 0.0;
        double totalWeight = 0.0; // для нормализации

        for (int i = 0; i < characteristicsList.size(); i++) {
            CharacteristicInfo ch = characteristicsList.get(i);

            // Пропускаем характеристики, которые отсутствуют у пациента
            if (!ch.availableInPatient) {
                continue;
            }

            float patientVal = patientValues[i];
            float trainVal = trainingValues[i];

            double contribution;

            if (ch.type == 1) {
                // Категориальная характеристика
                contribution = (Math.abs(trainVal - patientVal) < 0.5) ? ch.weight : 0.0;
            } else {
                // Числовая характеристика

                double diff = Math.abs(trainVal - patientVal);
                double range = ch.maxValue - ch.minValue;

                if (range <= 0) {
                    contribution = ch.weight;
                } else {
                    contribution = ch.weight * (range - diff) / range;
                    contribution = Math.max(0.0, contribution);
                }
            }

            similarity += contribution;
            totalWeight += ch.weight;
        }

        // Нормализуем схожесть (если нужно)
        if (totalWeight > 0) {
            similarity = similarity / totalWeight * 100.0;
        }

        return similarity;
    }

    /**
     * Преобразует TrainingData в список TrainingRecord
     */
    private List<TrainingRecord> parseTrainingData(TrainingData trainingData) {
        List<TrainingRecord> records = new ArrayList<>();
        List<float[]> trainingVectors = trainingData.getTrainingRecords();

        // Вектор содержит: ID анкеты + характеристики + диагноз
        int valuesPerRecord = characteristicsList.size() + 2; // ID + характеристики + диагноз

        for (float[] vector : trainingVectors) {
            if (vector.length != valuesPerRecord) {
                continue; // Пропускаем записи с неверным количеством данных
            }

            TrainingRecord record = new TrainingRecord();
            record.values = new float[characteristicsList.size()];

            // Копируем значения характеристик (пропускаем ID анкеты)
            System.arraycopy(vector, 1, record.values, 0, characteristicsList.size());

            // Последнее значение - диагноз
            record.diagnosisCode = Math.round(vector[vector.length - 1]);

            // Добавляем только записи с допустимыми диагнозами
            if (validDiagnoses.containsKey(record.diagnosisCode)) {
                records.add(record);
            }
        }

        return records;
    }

    /**
     * Рассчитывает итоговые вероятности классов
     */
    private Map<Integer, Float> calculateProbabilities(ClassStat[] classes) {
        float total = 0f;
        float[] probs = new float[3];

        for (int i = 0; i < 3; i++) {
            probs[i] = (classes[i].count > 0)
                    ? (float)(classes[i].totalSimilarity / classes[i].count)
                    : 0f;
            total += probs[i];
        }

        Map<Integer, Float> result = new HashMap<>();
        if (total > 0f) {
            result.put(1, probs[0] / total);
            result.put(5, probs[1] / total);
            result.put(6, probs[2] / total);
        } else {
            result.put(1, 0f);
            result.put(5, 0f);
            result.put(6, 0f);
        }

        return result;
    }

    /**
     * Вспомогательный метод для получения информации о характеристиках
     */
    public List<CharacteristicInfo> getCharacteristicInfos() {
        return new ArrayList<>(characteristicsList);
    }

    private static class ClassStat {
        int count = 0;
        double totalSimilarity = 0.0;
    }
}