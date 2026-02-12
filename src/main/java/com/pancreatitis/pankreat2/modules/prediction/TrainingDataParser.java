// TrainingDataParser.java
package com.pancreatitis.pankreat2.modules.prediction;

import java.io.*;
import java.util.*;

public class TrainingDataParser {

    //private static final String VERSION_PREFIX = "TrainingDataV";

    /**
     * Парсит обучающие данные из входного потока
     * @param inputStream поток с данными обучающей таблицы
     * @return объект TextFileTrainingData
     */
    public static TextFileTrainingData parseFromFile(InputStream inputStream)
            throws IOException, DataFormatException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, "UTF-8"))) {

            // 1. Читаем версию данных
//            String versionLine = reader.readLine();
//            if (versionLine == null || versionLine.trim().isEmpty()) {
//                throw new DataFormatException("Файл обучающих данных пуст или не содержит версию");
//            }

            //String version = parseVersion(versionLine.trim());
            String version = "1";

            // 2. Читаем строку с идентификаторами характеристик
            String idsLine = reader.readLine();
            if (idsLine == null) {
                throw new DataFormatException("Отсутствует строка с идентификаторами характеристик");
            }

            int[] characteristicIds = parseCharacteristicIds(idsLine.trim());

            // 3. Читаем все обучающие записи
            List<float[]> trainingRecords = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // Пропускаем пустые строки
                }

                float[] record = parseTrainingRecord(line, characteristicIds.length);
                trainingRecords.add(record);
            }

            if (trainingRecords.isEmpty()) {
                throw new DataFormatException("Нет обучающих записей в файле");
            }

            return new TextFileTrainingData(version, characteristicIds, trainingRecords);

        } catch (NumberFormatException e) {
            throw new DataFormatException("Ошибка формата числа в данных: " + e.getMessage(), e);
        }
    }

    /**
     * Парсит версию данных из строки
     * Формат: "TrainingDataV3"
     */
//    private static String parseVersion(String versionLine) throws DataFormatException {
//        if (!versionLine.startsWith(VERSION_PREFIX)) {
//            throw new DataFormatException("Неверный формат версии. Ожидается: " +
//                    VERSION_PREFIX + "<номер>, получено: " + versionLine);
//        }
//
//        String versionNumber = versionLine.substring(VERSION_PREFIX.length());
//        if (versionNumber.isEmpty()) {
//            throw new DataFormatException("Отсутствует номер версии");
//        }
//
//        // Проверяем, что версия состоит из цифр
//        if (!versionNumber.matches("\\d+")) {
//            throw new DataFormatException("Номер версии должен быть числом: " + versionNumber);
//        }
//
//        return versionLine; // Возвращаем полную строку версии
//    }

    /**
     * Парсит строку с идентификаторами характеристик
     * Формат: -1 idc₁ idc₂ ... idcₙ -1
     */
    static int[] parseCharacteristicIds(String line) throws DataFormatException {
        String[] parts = line.split("\\s+");

        // Проверяем минимальную длину (должны быть как минимум -1 X -1)
        if (parts.length < 3) {
            throw new DataFormatException(
                    "Неверный формат строки с ID характеристик. Ожидается формат: -1 id1 id2 ... idn -1"
            );
        }

        // Проверяем первый и последний элемент на -1
        if (!"-1".equals(parts[0]) || !"-1".equals(parts[parts.length - 1])) {
            throw new DataFormatException(
                    "Строка с ID характеристик должна начинаться и заканчиваться -1"
            );
        }

        // Извлекаем ID характеристик (исключая первый и последний -1)
        int[] ids = new int[parts.length - 2];

        try {
            for (int i = 0; i < ids.length; i++) {
                ids[i] = Integer.parseInt(parts[i + 1]);

                // Проверяем, что ID положительный
                if (ids[i] <= 0) {
                    throw new DataFormatException(
                            "ID характеристики должен быть положительным числом: " + ids[i]
                    );
                }

                // Проверяем на уникальность ID
                for (int j = 0; j < i; j++) {
                    if (ids[j] == ids[i]) {
                        throw new DataFormatException(
                                "Найден дублирующийся ID характеристики: " + ids[i]
                        );
                    }
                }
            }
        } catch (NumberFormatException e) {
            throw new DataFormatException(
                    "Ошибка парсинга ID характеристики: " + e.getMessage(), e
            );
        }

        if (ids.length == 0) {
            throw new DataFormatException("Нет ID характеристик в строке");
        }

        return ids;
    }

    /**
     * Парсит строку с обучающей записью
     * Формат: val₁ val₂ ... valₙ diagnosisCode
     * где n = количество характеристик
     */
    static float[] parseTrainingRecord(String line, int expectedLength)
            throws DataFormatException {

        String[] parts = line.split("\\s+");

        // Проверяем длину (ID анкеты + значения характеристик + диагноз)
        if (parts.length != expectedLength + 2) {
            throw new DataFormatException(
                    String.format("Неверное количество значений в обучающей записи. " +
                                    "Ожидается: 1 (ID анкеты) + %d (характеристики) + 1 (диагноз) = %d, " +
                                    "получено: %d",
                            expectedLength, expectedLength + 2, parts.length)
            );
        }

        float[] record = new float[expectedLength + 2]; // ID + характеристики + диагноз

        try {
            // Парсим ID анкеты (первый элемент)
            int questionnaireId = Integer.parseInt(parts[0]);
            record[0] = questionnaireId;

            // Проверяем ID анкеты
            if (questionnaireId <= 0) {
                throw new DataFormatException(
                        "ID анкеты должен быть положительным числом: " + questionnaireId
                );
            }

            // Парсим значения характеристик
            for (int i = 1; i <= expectedLength; i++) {
                record[i] = Float.parseFloat(parts[i]);

                // Проверяем на корректность числовых значений
                if (Float.isNaN(record[i]) || Float.isInfinite(record[i])) {
                    throw new DataFormatException(
                            "Некорректное числовое значение в позиции " + i + ": " + parts[i]
                    );
                }
            }

            // Парсим код диагноза (последний элемент)
            int diagnosisCode = Integer.parseInt(parts[expectedLength + 1]);
            record[expectedLength + 1] = diagnosisCode;

            // Проверяем допустимые коды диагнозов (1, 5, 6)
            if (diagnosisCode != 1 && diagnosisCode != 5 && diagnosisCode != 6) {
                throw new DataFormatException(
                        "Недопустимый код диагноза: " + diagnosisCode +
                                ". Допустимые значения: 1, 5, 6"
                );
            }

        } catch (NumberFormatException e) {
            throw new DataFormatException(
                    "Ошибка парсинга числового значения: " + e.getMessage(), e
            );
        }

        return record;
    }

    /**
     * Валидирует соответствие данных версии
     */
    private static void validateDataConsistency(String version, int[] characteristicIds,
                                                List<float[]> trainingRecords)
            throws DataFormatException {

        // Для каждой версии могут быть свои правила валидации
        switch (version) {
            case "TrainingDataV3":
                validateV3Data(characteristicIds, trainingRecords);
                break;
            case "TrainingDataV2":
                validateV2Data(characteristicIds, trainingRecords);
                break;
            case "TrainingDataV1":
                validateV1Data(characteristicIds, trainingRecords);
                break;
            default:
                // Для неизвестных версий выполняем базовую валидацию
                validateBaseData(characteristicIds, trainingRecords);
        }
    }

    private static void validateV3Data(int[] characteristicIds, List<float[]> trainingRecords)
            throws DataFormatException {
        validateBaseData(characteristicIds, trainingRecords);

        // Дополнительные проверки для V3
        if (characteristicIds.length < 5) {
            throw new DataFormatException(
                    "Для версии V3 ожидается не менее 5 характеристик"
            );
        }

        // Проверяем, что все значения характеристик в разумных пределах
        for (float[] record : trainingRecords) {
            for (int i = 0; i < characteristicIds.length; i++) {
                float value = record[i];
                // Для числовых характеристик проверяем разумные пределы
                if (value < 0 || value > 1000) {
                    throw new DataFormatException(
                            String.format("Значение характеристики %d выходит за разумные пределы: %.2f",
                                    characteristicIds[i], value)
                    );
                }
            }
        }
    }

    private static void validateV2Data(int[] characteristicIds, List<float[]> trainingRecords)
            throws DataFormatException {
        validateBaseData(characteristicIds, trainingRecords);
    }

    private static void validateV1Data(int[] characteristicIds, List<float[]> trainingRecords)
            throws DataFormatException {
        validateBaseData(characteristicIds, trainingRecords);
    }

    private static void validateBaseData(int[] characteristicIds, List<float[]> trainingRecords)
            throws DataFormatException {

        if (characteristicIds.length == 0) {
            throw new DataFormatException("Нет характеристик в данных");
        }

        if (trainingRecords.isEmpty()) {
            throw new DataFormatException("Нет обучающих записей");
        }

        // Проверяем, что все записи имеют одинаковую длину
        int expectedLength = characteristicIds.length + 2; // ID анкеты + характеристики + диагноз
        for (int i = 0; i < trainingRecords.size(); i++) {
            float[] record = trainingRecords.get(i);
            if (record.length != expectedLength) {
                throw new DataFormatException(
                        String.format("Запись %d имеет неверную длину. " +
                                        "Ожидается: %d, получено: %d",
                                i, expectedLength, record.length)
                );
            }
        }

        // Проверяем уникальность ID анкет
        Set<Float> questionnaireIds = new HashSet<>();
        for (float[] record : trainingRecords) {
            float questionnaireId = record[0]; // Первый элемент - ID анкеты
            if (questionnaireIds.contains(questionnaireId)) {
                throw new DataFormatException(
                        String.format("Дублирующийся ID анкеты: %.0f", questionnaireId)
                );
            }
            questionnaireIds.add(questionnaireId);
        }

        // Проверяем статистику по диагнозам
        Map<Float, Integer> diagnosisCounts = new HashMap<>();
        for (float[] record : trainingRecords) {
            float diagnosis = record[record.length - 1];
            diagnosisCounts.put(diagnosis, diagnosisCounts.getOrDefault(diagnosis, 0) + 1);
        }

        // Должны быть представлены все 3 класса диагнозов
        if (diagnosisCounts.size() < 3) {
            throw new DataFormatException(
                    "Не все классы диагнозов представлены в данных. " +
                            "Ожидаемые: 1, 5, 6. Найдены: " + diagnosisCounts.keySet()
            );
        }
    }
}