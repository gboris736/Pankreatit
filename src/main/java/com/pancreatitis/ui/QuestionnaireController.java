package com.pancreatitis.ui;

import com.pancreatitis.models.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;

public class QuestionnaireController implements Initializable {

    @FXML
    private VBox characteristicsContainer;

    @FXML
    private Button addCharacteristic;

    // Хранение всех характеристик по ID
    private final Map<Integer, VBox> characteristicBlocks = new HashMap<>();
    private final Map<Integer, VBox> valuesContainers = new HashMap<>();

    // Тестовые данные характеристик (заглушка)
    private List<CharacteristicItem> mockCharacteristics;

    // Тестовые данные значений (заглушка)
    private List<CharacterizationValue> mockValues;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Инициализация тестовых данных
        initMockData();

        // Очищаем контейнер, если там есть статические элементы из FXML
        characteristicsContainer.getChildren().clear();

        // Создаем блоки для всех характеристик из тестовых данных
        for (CharacteristicItem characteristic : mockCharacteristics) {
            createCharacteristicBlock(characteristic);
        }

        // Добавляем значения к характеристикам
        for (CharacterizationValue value : mockValues) {
            addValueToCharacteristic(value.getIdCharacteristic(), value.getValue(), value.getLastModified());
        }
    }

    /**
     * Инициализация тестовых данных
     */
    private void initMockData() {
        // Создаем типы характеристик
        CharacteristicType typeNumeric = new CharacteristicType();
        typeNumeric.setId(1);
        typeNumeric.setName("Числовая");

        CharacteristicType typeNonNumeric = new CharacteristicType();
        typeNonNumeric.setId(2);
        typeNonNumeric.setName("Выбор из списка");

        // Создаем характеристики
        mockCharacteristics = new ArrayList<>();

        // Числовая характеристика: Температура
        CharacteristicItemNumeric temp = new CharacteristicItemNumeric();
        temp.setIdCharacteristic(1);
        temp.setIdType(1);
        temp.setName("Температура");
        temp.setHint("°C");
        temp.setCreatedAt("2024-01-15");
        mockCharacteristics.add(temp);

//        // Числовая характеристика: Давление (систолическое/диастолическое)
//        CharacteristicItemNumeric pressure = new CharacteristicItemNumeric();
//        pressure.setIdCharacteristic(2);
//        pressure.setIdType(1);
//        pressure.setName("Давление");
//        pressure.setHint("мм рт.ст.");
//        pressure.setCreatedAt("2024-01-15");
//        mockCharacteristics.add(pressure);
//
//        // Числовая характеристика: Пульс
//        CharacteristicItemNumeric pulse = new CharacteristicItemNumeric();
//        pulse.setIdCharacteristic(3);
//        pulse.setIdType(1);
//        pulse.setName("Пульс");
//        pulse.setHint("уд/мин");
//        pulse.setCreatedAt("2024-01-15");
//        mockCharacteristics.add(pulse);
//
//        // Числовая характеристика: Вес
//        CharacteristicItemNumeric weight = new CharacteristicItemNumeric();
//        weight.setIdCharacteristic(4);
//        weight.setIdType(1);
//        weight.setName("Вес");
//        weight.setHint("кг");
//        weight.setCreatedAt("2024-01-15");
//        mockCharacteristics.add(weight);

        // Нечисловая характеристика: Болевой синдром
        CharacteristicItemNonNumeric pain = new CharacteristicItemNonNumeric();
        pain.setIdCharacteristic(5);
        pain.setIdType(2);
        pain.setName("Болевой синдром");
        pain.setHint("Интенсивность боли");
        pain.setOptions(Arrays.asList("Нет", "Слабая", "Умеренная", "Сильная", "Невыносимая"));
        pain.setCreatedAt("2024-01-15");
        mockCharacteristics.add(pain);

        // Нечисловая характеристика: Локализация боли
        CharacteristicItemNonNumeric painLocation = new CharacteristicItemNonNumeric();
        painLocation.setIdCharacteristic(6);
        painLocation.setIdType(2);
        painLocation.setName("Локализация боли");
        painLocation.setHint("Где болит");
        painLocation.setOptions(Arrays.asList("Эпигастрий", "Левое подреберье", "Правое подреберье", "Опоясывающая", "Диффузная"));
        painLocation.setCreatedAt("2024-01-15");
        mockCharacteristics.add(painLocation);

        // Создаем значения для характеристик
        mockValues = new ArrayList<>();

        // Значения для температуры
        mockValues.add(createValue(1, "36.6", "2024-01-15"));
        mockValues.add(createValue(1, "36.8", "2024-01-16"));
        mockValues.add(createValue(1, "36.7", "2024-01-17"));
        mockValues.add(createValue(1, "37.1", "2024-01-18"));

        // Значения для давления
        mockValues.add(createValue(2, "120/80", "2024-01-15"));
        mockValues.add(createValue(2, "118/79", "2024-01-16"));
        mockValues.add(createValue(2, "122/82", "2024-01-17"));

        // Значения для пульса
        mockValues.add(createValue(3, "72", "2024-01-15"));
        mockValues.add(createValue(3, "75", "2024-01-16"));
        mockValues.add(createValue(3, "70", "2024-01-17"));

        // Значения для веса
        mockValues.add(createValue(4, "75.5", "2024-01-15"));
        mockValues.add(createValue(4, "75.2", "2024-01-16"));

        // Значения для болевого синдрома
        mockValues.add(createValue(5, "Умеренная", "2024-01-15"));
        mockValues.add(createValue(5, "Сильная", "2024-01-16"));
        mockValues.add(createValue(5, "Слабая", "2024-01-17"));

        // Значения для локализации боли
        mockValues.add(createValue(6, "Эпигастрий", "2024-01-15"));
        mockValues.add(createValue(6, "Левое подреберье", "2024-01-16"));
        mockValues.add(createValue(6, "Опоясывающая", "2024-01-17"));
    }

    /**
     * Вспомогательный метод для создания значения
     */
    private CharacterizationValue createValue(int characteristicId, String value, String date) {
        CharacterizationValue cv = new CharacterizationValue();
        cv.setIdCharacteristic(characteristicId);
        cv.setValue(value);
        cv.setLastModified(date);
        return cv;
    }

    /**
     * Создание блока характеристики на основе объекта CharacteristicItem
     * @param characteristic объект характеристики
     */
    public void createCharacteristicBlock(CharacteristicItem characteristic) {
        int id = characteristic.getIdCharacteristic();
        String name = characteristic.getName();
        String hint = characteristic.getHint();
        boolean isNumeric = characteristic instanceof CharacteristicItemNumeric;
        boolean isNonNumeric = characteristic instanceof CharacteristicItemNonNumeric;

        // Основной блок характеристики
        VBox characteristicBlock = new VBox(10);
        characteristicBlock.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; " +
                "-fx-background-radius: 5; -fx-padding: 15;" +
                "-fx-background-color: white;");

        // Верхняя строка с заголовком и кнопками
        BorderPane headerPane = new BorderPane();

        // Заголовок с ID, названием и подсказкой
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(name + " (ID: " + id + ")");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleBox.getChildren().add(titleLabel);

        // Кнопка информации (для нечисловых характеристик показываем список опций)
        if (isNonNumeric) {
            Button infoButton = new Button("?");
            infoButton.setStyle("-fx-background-radius: 15; -fx-background-color: #3498db; -fx-text-fill: white;");
            infoButton.setPrefWidth(30);
            infoButton.setPrefHeight(30);
            infoButton.setTooltip(new Tooltip("Варианты выбора: " +
                    String.join(", ", ((CharacteristicItemNonNumeric) characteristic).getOptions())));
            titleBox.getChildren().add(infoButton);
        }

        headerPane.setLeft(titleBox);

        // Кнопка добавления значения для этой характеристики
        Button addValueButton = new Button("Добавить значение");
        addValueButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addValueButton.setPrefWidth(150);

        // Сохраняем ID характеристики для использования в обработчике
        final int characteristicId = id;
        final CharacteristicItem currentChar = characteristic;

        addValueButton.setOnAction(event -> {
            // Здесь будет логика добавления нового значения
            // Пока просто добавляем пустое значение
            addValueToCharacteristic(characteristicId, "", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        });

        headerPane.setRight(addValueButton);

        // Контейнер для значений
        VBox valuesContainer = new VBox(10);
        valuesContainer.setId("values-" + id);

        characteristicBlock.getChildren().addAll(headerPane, valuesContainer);

        // Сохраняем в коллекции
        characteristicBlocks.put(id, characteristicBlock);
        valuesContainers.put(id, valuesContainer);

        // Добавляем в общий контейнер
        characteristicsContainer.getChildren().add(characteristicBlock);
    }

    /**
     * Добавление значения к характеристике
     * @param characteristicId ID характеристики
     * @param value Значение
     * @param date Дата заполнения
     */
    public void addValueToCharacteristic(int characteristicId, String value, String date) {
        VBox valuesContainer = valuesContainers.get(characteristicId);
        if (valuesContainer == null) {
            System.err.println("Характеристика с ID " + characteristicId + " не найдена");
            return;
        }

        // Создаем блок значения
        HBox valueBlock = createValueBlock(value, date);
        valuesContainer.getChildren().add(valueBlock);
    }

    /**
     * Создание блока значения
     */
    private HBox createValueBlock(String value, String date) {
        HBox valueBox = new HBox(10);
        valueBox.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 5; " +
                "-fx-background-radius: 5; -fx-padding: 10;");
        valueBox.setAlignment(Pos.CENTER_LEFT);

        // Поле для значения
        TextField valueField = new TextField(value);
        valueField.setPromptText("Значение");
        valueField.setMaxWidth(300);
        HBox.setHgrow(valueField, Priority.ALWAYS);

        // Поле для даты
        TextField dateField = new TextField(date);
        dateField.setPromptText("Дата заполнения");
        dateField.setPrefWidth(300);
        HBox.setHgrow(dateField, Priority.ALWAYS);

        valueBox.getChildren().addAll(valueField, dateField);
        return valueBox;
    }

    /**
     * Получение контейнера характеристик (для доступа извне)
     */
    public VBox getCharacteristicsContainer() {
        return characteristicsContainer;
    }

    /**
     * Проверка существования характеристики
     */
    public boolean hasCharacteristic(int id) {
        return characteristicBlocks.containsKey(id);
    }

    /**
     * Получение всех ID характеристик
     */
    public Set<Integer> getAllCharacteristicIds() {
        return characteristicBlocks.keySet();
    }
}