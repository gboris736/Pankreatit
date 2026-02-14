// TextFileTrainingData.java
package com.pancreatitis.pankreat2.modules.trainset;

import java.util.*;

public class TextFileTrainingData implements TrainingData {

    private String version;
    private int[] characteristicIds;
    private List<float[]> trainingRecords;
    private int[] diagnoses; // Кэшированные диагнозы для быстрого доступа

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

    private int[] appendCodeDiagnosis(int[] arr, int value) {
        int[] res = java.util.Arrays.copyOf(arr, arr.length + 1);
        res[arr.length] = value;
        return res;
    }


    private int[] removeCodeDiagnosisAt(int[] arr, int index) {
        if (index < 0 || index >= arr.length) return arr; // или бросить исключение
        int[] res = new int[arr.length - 1];
        System.arraycopy(arr, 0, res, 0, index);                          // левая часть
        System.arraycopy(arr, index + 1, res, index, arr.length - index - 1); // правая часть
        return res;
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

    @Override
    public boolean addRecord(float[] record, int codeDiagnosis) {
        try {
            // Проверяем корректность длины записи
            if (record.length != characteristicIds.length + 2) {
                throw new IllegalArgumentException(
                        String.format("Неверная длина записи. Ожидается: %d, получено: %d",
                                characteristicIds.length + 2, record.length)
                );
            }

            // Проверяем уникальность ID анкеты
            int newQuestionnaireId = (int) record[0];
            for (float[] existingRecord : trainingRecords) {
                if ((int) existingRecord[0] == newQuestionnaireId) {
                    return false;
                }
            }

            trainingRecords.add(record.clone());
            appendCodeDiagnosis(diagnoses, codeDiagnosis);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean deleteRecord(long questionnaireId) {
        try {
            // Ищем индекс записи с указанным ID
            int indexToRemove = -1;
            for (int i = 0; i < trainingRecords.size(); i++) {
                if ((int) trainingRecords.get(i)[0] == (int) questionnaireId) {
                    indexToRemove = i;
                    break;
                }
            }

            if (indexToRemove == -1) {
                return false; // Запись не найдена
            }

            trainingRecords.remove(indexToRemove);
            removeCodeDiagnosisAt(diagnoses, indexToRemove);

            return true;
        } catch (Exception e) {
            return false;
        }
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

    @Override
    public String toString() {
        return String.format(
                "TrainingData{version=%s, characteristics=%d, records=%d}",
                version, characteristicIds.length, trainingRecords.size()
        );
    }
}