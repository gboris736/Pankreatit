package com.pancreatitis.ui;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.database.DatabaseModule;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class QuestionnaireController {

    @FXML private VBox characteristicsContainer;
    @FXML private ComboBox<String> diagnosis;
    @FXML private TextField fio;
    @FXML private TextField addmitedFrom;
    @FXML private TextField createdAt;
    @FXML private Button btnBack;
    @FXML private Button btnSave;

    private int idQuestionnaire;
    private int idPatient;
    private Questionnaire questionnaire;
    private Patient patient;

    // Все характеристики, упорядоченные по ID
    private final Map<Integer, Characteristic> characteristicsMap = new LinkedHashMap<>();

    // Все значения, сгруппированные по ID характеристики (отсортированы по убыванию даты)
    private final Map<Integer, List<CharacterizationAnketPatient>> valuesByCharacteristic = new LinkedHashMap<>();

    // Множество новых значений (ещё не сохранённых в БД)
    private final Set<CharacterizationAnketPatient> newValues = new HashSet<>();

    // Для нечисловых характеристик: справочные данные
    private final Map<Integer, Map<Integer, String>> optionIdToText = new HashMap<>();
    private final Map<Integer, Map<String, Integer>> optionTextToId = new HashMap<>();
    private final Map<Integer, List<String>> optionTexts = new HashMap<>();

    // Контейнеры для блоков значений (ключ - ID характеристики)
    private final Map<Integer, VBox> valuesContainers = new HashMap<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        initData();
        settingsCommonField();
        buildAllCharacteristicBlocks();
        populateAllValues();
    }

    private void initData() {
        DatabaseModule db = DatabaseModule.getInstance();

        idQuestionnaire = MainMenuControl.idCurrentQuestionnaire;
        idPatient = MainMenuControl.idCurrentPatient;

        if (idQuestionnaire == -1 || idPatient == -1) {
            // Новая анкета
            questionnaire = new Questionnaire();
            patient = db.getPatientById(idPatient);
            if (patient == null) patient = new Patient();
        } else {
            questionnaire = db.getQuestionnaireById(idQuestionnaire);
            patient = db.getPatientById(idPatient);
        }

        // Загружаем все характеристики
        List<Characteristic> allCharacteristics = db.getAllCharacteristics();
        for (Characteristic c : allCharacteristics) {
            characteristicsMap.put(c.getId(), c);
            valuesByCharacteristic.put(c.getId(), new ArrayList<>());

            if (c.getIdType() != 3) {
                // Загружаем справочные значения
                List<CharacterizationValue> charValues = db.getValuesForCharacteristic(c.getId());
                Map<Integer, String> idToText = new HashMap<>();
                Map<String, Integer> textToId = new HashMap<>();
                List<String> texts = new ArrayList<>();

                idToText.put(0, "Нет данных");
                textToId.put("Нет данных", 0);
                texts.add("Нет данных");

                for (CharacterizationValue cv : charValues) {
                    idToText.put((int)cv.getId(), cv.getValue());
                    textToId.put(cv.getValue(), (int)cv.getId());
                    texts.add(cv.getValue());
                }

                optionIdToText.put(c.getId(), idToText);
                optionTextToId.put(c.getId(), textToId);
                optionTexts.put(c.getId(), texts);
            }
        }

        if (idQuestionnaire != -1) {
            // Загружаем существующие значения для анкеты
            List<CharacterizationAnketPatient> existingValues = db.getCharacterizationsForAnket(idQuestionnaire);
            for (CharacterizationAnketPatient cap : existingValues) {
                int charId = cap.getIdCharacteristic();
                List<CharacterizationAnketPatient> list = valuesByCharacteristic.get(charId);
                if (list != null) {
                    list.add(cap);
                }
            }
        } else {
            // Создаём первичные значения для новой анкеты (все помечаются как новые)
            for (Characteristic c : characteristicsMap.values()) {
                CharacterizationAnketPatient cap = new CharacterizationAnketPatient();
                cap.setIdAnket(-1);
                cap.setIdCharacteristic(c.getId());
                cap.setCreatedAt(FORMATTER.format(LocalDateTime.now()));

                if (c.getIdType() == 3) {
                    cap.setValue(-1f);
                } else {
                    cap.setIdValue(0); // "Нет данных"
                }

                valuesByCharacteristic.get(c.getId()).add(cap);
                newValues.add(cap); // помечаем как новое
            }
        }

        // Сортируем значения каждой характеристики по дате (новые сверху)
        for (int charId : valuesByCharacteristic.keySet()) {
            List<CharacterizationAnketPatient> list = valuesByCharacteristic.get(charId);
            list.sort((a, b) -> {
                try {
                    LocalDateTime da = LocalDateTime.parse(a.getCreatedAt(), FORMATTER);
                    LocalDateTime db_ = LocalDateTime.parse(b.getCreatedAt(), FORMATTER);
                    return db_.compareTo(da);
                } catch (Exception e) {
                    return 0;
                }
            });
        }

        btnBack.setOnAction(event -> {
            MainMenuControl mainMenuControl = MainMenuControl.getInstance();
            mainMenuControl.showViewForTab("Список анкет");
        });

        btnSave.setOnAction(event -> {
            List<CharacterizationAnketPatient> characterizationAnketPatients = getNewValues();
            try {
                for(CharacterizationAnketPatient characterizationAnketPatient: characterizationAnketPatients){
                    System.out.println(characterizationAnketPatient.toJson());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void settingsCommonField() {
        fio.setText(patient != null ? patient.getFio() : "");
        createdAt.setText(questionnaire != null ? questionnaire.getDateOfCompletion() : "");
        addmitedFrom.setText(questionnaire != null ? questionnaire.getAdmittedFrom() : "");

        String diag = (questionnaire != null && questionnaire.getDiagnosis() != null) ? questionnaire.getDiagnosis() : "-";
        diagnosis.setValue("-".equals(diag) ? "Нет данных" : diag);
    }

    /** Создаёт визуальные блоки для всех характеристик */
    private void buildAllCharacteristicBlocks() {
        for (Characteristic c : characteristicsMap.values()) {
            VBox block = createCharacteristicBlock(c);
            characteristicsContainer.getChildren().add(block);
        }
    }

    /** Создаёт блок одной характеристики */
    private VBox createCharacteristicBlock(Characteristic c) {
        int charId = c.getId();
        String name = c.getOpis();
        String hint = c.getHints();

        VBox block = new VBox(10);
        block.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; " +
                "-fx-background-radius: 5; -fx-padding: 15; -fx-background-color: white;");

        BorderPane header = new BorderPane();
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(name);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Button infoBtn = new Button("?");
        infoBtn.setStyle("-fx-background-radius: 15; -fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        infoBtn.setPrefSize(30, 30);
        infoBtn.setOnAction(e -> showHintDialog(name, hint));

        titleBox.getChildren().addAll(titleLabel, infoBtn);
        header.setLeft(titleBox);

        Button addBtn = new Button("Добавить значение");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addBtn.setPrefWidth(180);
        addBtn.setOnAction(e -> addNewValue(charId));
        header.setRight(addBtn);

        VBox valuesContainer = new VBox(10);
        valuesContainer.setId("values-" + charId);
        valuesContainers.put(charId, valuesContainer);

        block.getChildren().addAll(header, valuesContainer);
        return block;
    }

    /** Заполняет контейнеры значений для всех характеристик */
    private void populateAllValues() {
        for (int charId : valuesContainers.keySet()) {
            refreshCharacteristicValues(charId);
        }
    }

    /** Обновляет отображение значений для конкретной характеристики */
    private void refreshCharacteristicValues(int characteristicId) {
        VBox container = valuesContainers.get(characteristicId);
        if (container == null) return;

        container.getChildren().clear();

        List<CharacterizationAnketPatient> values = valuesByCharacteristic.get(characteristicId);
        if (values == null) return;

        Characteristic c = characteristicsMap.get(characteristicId);
        for (CharacterizationAnketPatient cap : values) {
            HBox valueBlock = createValueBlock(c, cap);
            container.getChildren().add(valueBlock);
        }
    }

    /** Создаёт блок одного значения */
    private HBox createValueBlock(Characteristic c, CharacterizationAnketPatient cap) {
        if (c.getIdType() == 3) {
            return createNumericValueBlock(cap);
        } else {
            return createNonNumericValueBlock(c, cap);
        }
    }

    private HBox createNumericValueBlock(CharacterizationAnketPatient cap) {
        HBox box = new HBox(10);
        box.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 5; " +
                "-fx-background-radius: 5; -fx-padding: 10;");
        box.setAlignment(Pos.CENTER_LEFT);

        boolean isNew = newValues.contains(cap);

        TextField valueField = new TextField(cap.getValue() == -1 ? "" : String.valueOf(cap.getValue()));
        valueField.setPromptText("Числовое значение");
        valueField.setMaxWidth(300);
        valueField.setEditable(isNew); // Только новые можно редактировать
        if (!isNew) {
            valueField.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555;");
        }
        HBox.setHgrow(valueField, Priority.ALWAYS);

        TextField dateField = new TextField(cap.getCreatedAt());
        dateField.setPromptText("Дата заполнения");
        dateField.setPrefWidth(300);
        dateField.setEditable(false);
        dateField.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555;");
        HBox.setHgrow(dateField, Priority.ALWAYS);

        if (isNew) {
            // Обновление модели при потере фокуса
            valueField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    try {
                        float val = valueField.getText().isEmpty() ? -1 : Float.parseFloat(valueField.getText());
                        cap.setValue(val);
                    } catch (NumberFormatException ex) {
                        valueField.setText(String.valueOf(cap.getValue()));
                    }
                }
            });

            // Слушатель изменения текста (после того как пользователь ввел значение)
            valueField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.equals(oldVal)) {
                    String currentTime = FORMATTER.format(LocalDateTime.now());
                    dateField.setText(currentTime);
                    cap.setCreatedAt(currentTime);
                }
            });
        }

        box.getChildren().addAll(valueField, dateField);
        return box;
    }

    private HBox createNonNumericValueBlock(Characteristic c, CharacterizationAnketPatient cap) {
        int charId = c.getId();
        HBox box = new HBox(10);
        box.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 5; " +
                "-fx-background-radius: 5; -fx-padding: 10;");
        box.setAlignment(Pos.CENTER_LEFT);

        boolean isNew = newValues.contains(cap);

        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(optionTexts.get(charId));
        combo.setPromptText("Выберите значение");
        combo.setMaxWidth(300);
        combo.setEditable(isNew);
        if (!isNew) {
            combo.setStyle("-fx-background-color: #f5f5f5;");
        }
        HBox.setHgrow(combo, Priority.ALWAYS);

        String currentText = optionIdToText.get(charId).get(cap.getIdValue());
        if (currentText != null) {
            combo.setValue(currentText);
        }

        TextField dateField = new TextField(cap.getCreatedAt());
        dateField.setPromptText("Дата заполнения");
        dateField.setPrefWidth(300);
        dateField.setEditable(false);
        dateField.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555;");
        HBox.setHgrow(dateField, Priority.ALWAYS);

        if (isNew) {
            // Обновление модели при изменении выбора
            combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    Integer id = optionTextToId.get(charId).get(newVal);
                    if (id != null) {
                        cap.setIdValue(id);
                    }
                }
            });

            // Слушатель изменения выбора в ComboBox
            combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals(oldVal)) {
                    String currentTime = FORMATTER.format(LocalDateTime.now());
                    dateField.setText(currentTime);
                    cap.setCreatedAt(currentTime);

                    Integer id = optionTextToId.get(charId).get(newVal);
                    if (id != null) {
                        cap.setIdValue(id);
                    }
                }
            });
        }

        box.getChildren().addAll(combo, dateField);
        return box;
    }

    /** Добавляет новое значение для указанной характеристики */
    private void addNewValue(int characteristicId) {
        Characteristic c = characteristicsMap.get(characteristicId);
        if (c == null) return;

        CharacterizationAnketPatient cap = new CharacterizationAnketPatient();
        cap.setIdAnket(idQuestionnaire == -1 ? -1 : idQuestionnaire);
        cap.setIdCharacteristic(characteristicId);
        cap.setCreatedAt(FORMATTER.format(LocalDateTime.now()));

        if (c.getIdType() == 3) {
            cap.setValue(-1f);
        } else {
            cap.setIdValue(0); // "Нет данных"
        }

        // Добавляем в начало списка (как самое новое)
        valuesByCharacteristic.get(characteristicId).add(0, cap);
        newValues.add(cap); // помечаем как новое

        refreshCharacteristicValues(characteristicId);
    }

    private void showHintDialog(String characteristicName, String hint) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Подсказка");
        dialog.setHeaderText("Характеристика: " + characteristicName);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);

        Label hintLabel = new Label(hint);
        hintLabel.setWrapText(true);
        hintLabel.setStyle("-fx-font-size: 14px;");

        content.getChildren().add(hintLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    // ================== Методы доступа к данным ==================

    /** Возвращает все значения (как существующие, так и новые) */
    public List<CharacterizationAnketPatient> getAllValues() {
        return valuesByCharacteristic.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /** Возвращает только новые значения (созданные в текущей сессии) */
    public List<CharacterizationAnketPatient> getNewValues() {
        return new ArrayList<>(newValues);
    }

    /** Возвращает последние значения для каждой характеристики */
    public List<CharacterizationAnketPatient> getLatestValues() {
        List<CharacterizationAnketPatient> result = new ArrayList<>();
        for (List<CharacterizationAnketPatient> list : valuesByCharacteristic.values()) {
            if (!list.isEmpty()) {
                result.add(list.get(0));
            }
        }
        return result;
    }

    // Геттеры для полей анкеты
    public String getFio() { return fio.getText(); }
    public String getAdmittedFrom() { return addmitedFrom.getText(); }
    public String getDiagnosis() { return diagnosis.getValue(); }
    public String getCreatedAt() { return createdAt.getText(); }
}