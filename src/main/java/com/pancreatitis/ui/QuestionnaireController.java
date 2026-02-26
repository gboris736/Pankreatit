package com.pancreatitis.ui;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.database.DatabaseModule;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class QuestionnaireController implements Initializable {

    @FXML
    private VBox characteristicsContainer;

    private int idQuestionnaire = 118;
    private int idPatient = 1;

    // Хранение всех характеристик по ID
    private final Map<Integer, VBox> characteristicBlocks = new HashMap<>();
    private final Map<Integer, VBox> valuesContainers = new HashMap<>();

    private List<CharacteristicItem> characteristicItems = new ArrayList<>();
    private List<CharacterizationAnketPatient> characterizationAnketPatientList = new ArrayList<>();
    private List<Characteristic> characteristics = new ArrayList<>();

    private HashMap<Integer, CharacterizationAnketPatient> hashMap = new HashMap<>();
    private HashMap<Integer, List<String>> hashMapOptions = new HashMap<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static boolean compare(String a, String b) throws DateTimeParseException {
        try {
            if (b == null || b.isEmpty()) return true;
            LocalDateTime dtA = LocalDateTime.parse(a, FORMATTER);
            LocalDateTime dtB = LocalDateTime.parse(b, FORMATTER);
            return !dtA.isBefore(dtB);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Инициализация данных
        initData();

        // Создаем блоки для всех характеристи
        for (CharacteristicItem characteristic : characteristicItems) {
            createCharacteristicBlock(characteristic);
        }

        // Добавляем значения к характеристикам
        for (CharacterizationAnketPatient characterizationAnketPatient : characterizationAnketPatientList) {
            int characteristicId = characterizationAnketPatient.getIdCharacteristic();
            List<String> options = hashMapOptions.get(characteristicId);
            Characteristic characteristic = characteristics.get(characteristicId);
            if (characteristic.getIdType() != 3) {
                addNonNumericValueToCharacteristic(characteristicId, characterizationAnketPatient.getIdValue(), characterizationAnketPatient.getCreatedAt(), options);
            } else {
                addValueToCharacteristic(characteristicId, Float.toString(characterizationAnketPatient.getValue()), characterizationAnketPatient.getCreatedAt());
            }
        }
    }

    /**
     * Инициализация данных
     */
    private void initData() {
        DatabaseModule databaseModule = DatabaseModule.getInstance();

        characterizationAnketPatientList = databaseModule.getCharacterizationsForAnket(idQuestionnaire);
        characteristics = databaseModule.getAllCharacteristics();

        for(Characteristic characteristic: characteristics){
            characteristicItems.add(new CharacteristicItem(characteristic));

            CharacterizationAnketPatient characterizationAnketPatient = new CharacterizationAnketPatient();
            characterizationAnketPatient.setIdAnket(idQuestionnaire);
            characterizationAnketPatient.setIdCharacteristic(characteristic.getId());

            hashMap.put(characteristic.getId(), characterizationAnketPatient);

            if (characteristic.getIdType() != 3) {
                List<CharacterizationValue> characterizationValues = databaseModule.getValuesForCharacteristic(characteristic.getId());
                List<String> options = new ArrayList<>();
                options.add("Нет данных");
                for(CharacterizationValue characterizationValue: characterizationValues){
                    options.add(characterizationValue.getValue());
                }

                hashMapOptions.put(characteristic.getId(), options);
            }
        }

        // В мапе держатся самые последние значения для каждой характеристики
        for(CharacterizationAnketPatient characterizationAnketPatient: characterizationAnketPatientList){
             if(compare(characterizationAnketPatient.getCreatedAt(), hashMap.get(characterizationAnketPatient.getIdCharacteristic()).getCreatedAt())) {
                  hashMap.put(characterizationAnketPatient.getIdCharacteristic(), characterizationAnketPatient);
             }
        }
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
        boolean isNonNumeric = characteristic.getIdType() != 3;

        // Основной блок характеристики
        VBox characteristicBlock = new VBox(10);
        characteristicBlock.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; " +
                "-fx-background-radius: 5; -fx-padding: 15;" +
                "-fx-background-color: white;");

        // Верхняя строка с заголовком
        BorderPane headerPane = new BorderPane();

        // Заголовок с ID, названием и подсказкой
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(name);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        titleBox.getChildren().add(titleLabel);

        // Кнопка информации (для нечисловых характеристик показываем список опций)
        Button infoButton = new Button("?");
        infoButton.setStyle("-fx-background-radius: 15; -fx-background-color: #3498db; -fx-text-fill: white;");
        infoButton.setPrefWidth(30);
        infoButton.setPrefHeight(30);
        infoButton.setTooltip(new Tooltip(hint));
        titleBox.getChildren().add(infoButton);

        headerPane.setLeft(titleBox);

        // Кнопка добавления значения для этой характеристики
        Button addValueButton = new Button("Добавить значение");
        addValueButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addValueButton.setPrefWidth(180);

        final int characteristicId = id;
        final CharacteristicItem currentChar = characteristic;
        final boolean isNonNumericFinal = isNonNumeric;

        addValueButton.setOnAction(event -> {
            // Для нечисловых характеристик добавляем пустое значение с выбором из списка
            if (isNonNumericFinal) {
                addNonNumericValueToCharacteristic(characteristicId, 0, "-", hashMapOptions.get(characteristicId));
            } else {
                addValueToCharacteristic(characteristicId, "","-");
            }
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
     * Добавление числового значения к характеристике
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

        // Создаем блок числового значения
        HBox valueBlock = createNumericValueBlock(value, date);
        valuesContainer.getChildren().add(valueBlock);
    }

    /**
     * Добавление нечислового значения к характеристике (с выпадающим списком)
     * @param characteristicId ID характеристики
     * @param optionIndex Значение
     * @param date Дата заполнения
     * @param options Список возможных вариантов
     */
    public void addNonNumericValueToCharacteristic(int characteristicId, int optionIndex, String date, List<String> options) {
        VBox valuesContainer = valuesContainers.get(characteristicId);
        if (valuesContainer == null) {
            System.err.println("Характеристика с ID " + characteristicId + " не найдена");
            return;
        }

        // Создаем блок нечислового значения
        HBox valueBlock = createNonNumericValueBlock(optionIndex, date, options);
        valuesContainer.getChildren().add(valueBlock);
    }

    /**
     * Создание блока числового значения
     */
    private HBox createNumericValueBlock(String value, String date) {
        HBox valueBox = new HBox(10);
        valueBox.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 5; " +
                "-fx-background-radius: 5; -fx-padding: 10;");
        valueBox.setAlignment(Pos.CENTER_LEFT);

        // Поле для числового значения
        TextField valueField = new TextField(value);
        valueField.setPromptText("Числовое значение");
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
     * Создание блока нечислового значения с выпадающим списком
     */
    private HBox createNonNumericValueBlock(int optionIndex, String date, List<String> options) {
        HBox valueBox = new HBox(10);
        valueBox.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 5; " +
                "-fx-background-radius: 5; -fx-padding: 10;");
        valueBox.setAlignment(Pos.CENTER_LEFT);

        // Выпадающий список для выбора значения
        ComboBox<String> valueCombo = new ComboBox<>();
        valueCombo.getItems().addAll(options);
        valueCombo.setPromptText("Выберите значение");
        valueCombo.setMaxWidth(300);
        valueCombo.setEditable(false);
        HBox.setHgrow(valueCombo, Priority.ALWAYS);

        // Если передано значение, пытаемся его выбрать
        if (optionIndex >= 0 && optionIndex < options.size()) {
            valueCombo.setValue(options.get(optionIndex));
        }

        // Поле для даты
        TextField dateField = new TextField(date);
        dateField.setPromptText("Дата заполнения");
        dateField.setPrefWidth(300);
        HBox.setHgrow(dateField, Priority.ALWAYS);

        valueBox.getChildren().addAll(valueCombo, dateField);
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