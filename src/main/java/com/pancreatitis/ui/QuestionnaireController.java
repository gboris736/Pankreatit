package com.pancreatitis.ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;

public class QuestionnaireController {
    @FXML
    private VBox characteristicsContainer;

    // Хранение всех характеристик по ID
    private final Map<Integer, VBox> characteristicBlocks = new HashMap<>();
    private final Map<Integer, VBox> valuesContainers = new HashMap<>();

    @FXML
    public void initialize() {
        // Создание блоков характеристик
        createCharacteristicBlock(1, "Температура");
        createCharacteristicBlock(2, "Давление");
        createCharacteristicBlock(3, "Пульс");
        createCharacteristicBlock(4, "Вес");

        // Добавление значений по ID характеристики
        addValueToCharacteristic(1, "36.6", "2024-01-15");
        addValueToCharacteristic(1, "36.8", "2024-01-16");
        addValueToCharacteristic(1, "36.7", "2024-01-17");

        addValueToCharacteristic(2, "120/80", "2024-01-15");
        addValueToCharacteristic(2, "118/79", "2024-01-16");

        addValueToCharacteristic(3, "72", "2024-01-15");
        addValueToCharacteristic(3, "75", "2024-01-16");

        addValueToCharacteristic(4, "75.5", "2024-01-15");
    }

    /**
     * Создание блока характеристики
     * @param id ID характеристики
     * @param name Название характеристики
     */
    public void createCharacteristicBlock(int id, String name) {
        // Основной блок характеристики
        VBox characteristicBlock = new VBox(10);
        characteristicBlock.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; " +
                "-fx-background-radius: 5; -fx-padding: 15;" +
                "-fx-background-color: white;");

        // Заголовок с ID и названием
        Label titleLabel = new Label(name + " (ID: " + id + ")");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Контейнер для значений
        VBox valuesContainer = new VBox(10);
        valuesContainer.setId("values-" + id);

        characteristicBlock.getChildren().addAll(titleLabel, valuesContainer);

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
        valueBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Поле для значения
        TextField valueField = new TextField(value);
        valueField.setPromptText("Значение");
        valueField.setPrefWidth(300);
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
