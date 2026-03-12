package com.pancreatitis.ui;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.prediction.PredictionModule;
import com.pancreatitis.modules.prediction.PredictionResult;
import com.pancreatitis.modules.updates.UpdatesModule;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class QuestionnaireViewUpdate {
    @FXML private VBox characteristicsContainer;
    @FXML private ComboBox<String> diagnosisCombo;
    @FXML private TextField fio;
    @FXML private TextField addmitedFrom;
    @FXML private TextField createdAt;
    @FXML private Button btnBack;
    @FXML private Button btnSave;
    @FXML private Button btnPredict;

    // Объекты, переданные извне
    private Questionnaire questionnaire;
    private Patient patient;
    private List<CharacterizationAnketPatient> allValues;
    private List<Characteristic> allCharacteristics;            // все возможные характеристики
    private Map<Integer, List<CharacterizationValue>> referenceValues = new HashMap<>();

    // Внутренние структуры для быстрого доступа
    private final Map<Integer, Characteristic> characteristicsMap = new LinkedHashMap<>();
    private final Map<Integer, List<CharacterizationAnketPatient>> valuesByCharacteristic = new LinkedHashMap<>();
    private final Set<CharacterizationAnketPatient> newValues = new HashSet<>(); // значения, созданные в этой сессии

    // Справочные данные для нечисловых характеристик
    private final Map<Integer, Map<Integer, String>> optionIdToText = new HashMap<>();
    private final Map<Integer, Map<String, Integer>> optionTextToId = new HashMap<>();
    private final Map<Integer, List<String>> optionTexts = new HashMap<>();

    // Контейнеры для блоков значений (ключ - ID характеристики)
    private final Map<Integer, VBox> valuesContainers = new HashMap<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, String> DIAGNOSIS_MAP = new HashMap<>();

    static {
        DIAGNOSIS_MAP.put("Отечный панкреатит", "1");
        DIAGNOSIS_MAP.put("Панкреонекроз среднетяжелое течение", "5");
        DIAGNOSIS_MAP.put("Панкреонекроз тяжелое течение", "6");
    }

    static int id;

    @FXML
    public void initialize() {
        setData();
        buildInternalStructures();
        initUI();
        setupActions();
    }

    public void setData() {
        UpdatesModule updatesModule = UpdatesModule.getInstance();
        questionnaire = updatesModule.getQuestionnairList().get(id);
        patient = updatesModule.getPatientList().get(id);
        allValues = updatesModule.getCharacterizationAnketPatientList().get(id);

        DatabaseModule databaseModule = DatabaseModule.getInstance();
        allCharacteristics = databaseModule.getAllCharacteristics();
        for(Characteristic ch: allCharacteristics){
            if (ch.getIdType() != 3){
                referenceValues.put(ch.getId(), databaseModule.getValuesForCharacteristic(ch.getId()));
            }
        }
    }

    // ------------------- Внутренняя инициализация -------------------

    /** Создаёт внутренние маппинги на основе переданных данных */
    private void buildInternalStructures() {
        // Заполняем characteristicsMap
        for (Characteristic c : allCharacteristics) {
            characteristicsMap.put(c.getId(), c);
            valuesByCharacteristic.put(c.getId(), new ArrayList<>());
        }

        // Группируем значения по характеристикам
        for (CharacterizationAnketPatient val : allValues) {
            int charId = val.getIdCharacteristic();
            List<CharacterizationAnketPatient> list = valuesByCharacteristic.get(charId);
            if (list != null) {
                list.add(val);
            }
        }

        // Сортируем значения каждой характеристики по дате (новые сверху)
        for (List<CharacterizationAnketPatient> list : valuesByCharacteristic.values()) {
            list.sort((a, b) -> {
                try {
                    LocalDateTime da = LocalDateTime.parse(a.getCreatedAt(), FORMATTER);
                    LocalDateTime db = LocalDateTime.parse(b.getCreatedAt(), FORMATTER);
                    return db.compareTo(da);
                } catch (Exception e) {
                    return 0;
                }
            });
        }

        // Если анкета новая (id = -1), создаём значения по умолчанию для всех характеристик
        if (questionnaire.getId() == -1 && allValues.isEmpty()) {
            for (Characteristic c : allCharacteristics) {
                CharacterizationAnketPatient cap = new CharacterizationAnketPatient();
                cap.setIdAnket(-1);
                cap.setIdCharacteristic(c.getId());
                cap.setCreatedAt(FORMATTER.format(LocalDateTime.now()));

                if (c.getIdType() == 3) { // числовая
                    cap.setValue(-1f);
                } else {
                    cap.setIdValue(0); // "Нет данных"
                }

                allValues.add(cap);
                valuesByCharacteristic.get(c.getId()).add(cap);
                newValues.add(cap);
            }
        }

        // Загружаем справочные данные для нечисловых характеристик
        for (Characteristic c : allCharacteristics) {
            if (c.getIdType() != 3) {
                List<CharacterizationValue> charValues = referenceValues.getOrDefault(c.getId(), Collections.emptyList());
                Map<Integer, String> idToText = new HashMap<>();
                Map<String, Integer> textToId = new HashMap<>();
                List<String> texts = new ArrayList<>();

                idToText.put(0, "Нет данных");
                textToId.put("Нет данных", 0);
                texts.add("Нет данных");

                for (CharacterizationValue cv : charValues) {
                    idToText.put((int) cv.getIdValue(), cv.getValue());
                    textToId.put(cv.getValue(), (int) cv.getIdValue());
                    texts.add(cv.getValue());
                }

                optionIdToText.put(c.getId(), idToText);
                optionTextToId.put(c.getId(), textToId);
                optionTexts.put(c.getId(), texts);
            }
        }
    }

    /** Заполняет UI на основе внутренних данных */
    private void initUI() {
        // Общие поля
        fio.setText(patient.getFio() != null ? patient.getFio() : "");
        fio.setEditable(false); // как в оригинале, ФИО не редактируется

        createdAt.setText(questionnaire.getDateOfCompletion() != null ? questionnaire.getDateOfCompletion() : "");

        addmitedFrom.setText(questionnaire.getAdmittedFrom() != null ? questionnaire.getAdmittedFrom() : "");

        String diagText = (questionnaire.getTextDiagnosis() != null && !"-".equals(questionnaire.getTextDiagnosis()))
                ? questionnaire.getTextDiagnosis() : "Нет данных";
        diagnosisCombo.setValue(diagText);

        // Блоки характеристик
        for (Characteristic c : allCharacteristics) {
            VBox block = createCharacteristicBlock(c);
            characteristicsContainer.getChildren().add(block);
        }

        // Заполняем значения
        for (int charId : valuesContainers.keySet()) {
            refreshCharacteristicValues(charId);
        }
    }

    private void setupActions() {
        btnBack.setOnAction(e -> handleBack());
        btnSave.setOnAction(e -> handleSave());
        btnPredict.setOnAction(e -> handlePredict());
    }

    // ------------------- Построение UI для характеристик -------------------

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
        valueField.setDisable(!isNew);
        HBox.setHgrow(valueField, Priority.ALWAYS);

        TextField dateField = new TextField(cap.getCreatedAt());
        dateField.setPromptText("Дата заполнения");
        dateField.setPrefWidth(300);
        dateField.setEditable(false);
        dateField.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555;");
        HBox.setHgrow(dateField, Priority.ALWAYS);

        if (isNew) {
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
        combo.setDisable(!isNew);
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
            combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    Integer id = optionTextToId.get(charId).get(newVal);
                    if (id != null) {
                        cap.setIdValue(id);
                    }
                }
            });

            combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals(oldVal)) {
                    String currentTime = FORMATTER.format(LocalDateTime.now());
                    dateField.setText(currentTime);
                    cap.setCreatedAt(currentTime);
                }
            });
        }

        box.getChildren().addAll(combo, dateField);
        return box;
    }

    private void addNewValue(int characteristicId) {
        Characteristic c = characteristicsMap.get(characteristicId);
        if (c == null) return;

        CharacterizationAnketPatient cap = new CharacterizationAnketPatient();
        cap.setIdAnket(questionnaire.getId()); // может быть -1
        cap.setIdCharacteristic(characteristicId);
        cap.setCreatedAt(FORMATTER.format(LocalDateTime.now()));

        if (c.getIdType() == 3) {
            cap.setValue(-1f);
        } else {
            cap.setIdValue(0);
        }

        // Добавляем в начало списка и в общий список allValues
        valuesByCharacteristic.get(characteristicId).add(0, cap);
        allValues.add(cap);
        newValues.add(cap);

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

    // ------------------- Обработка действий -------------------

    private void handleBack() {
        MainMenuControl mainMenuControl = MainMenuControl.getInstance();
        mainMenuControl.showViewForTab("Анкеты на верификацию");
    }

    private void handleSave() {
        // Обновляем объект анкеты
        questionnaire.setDiagnosis(diagnosisToCode(diagnosisCombo.getValue()));
        //questionnaire.setDiagnosis(DIAGNOSIS_MAP.get(diagnosisCombo.getValue()));
        questionnaire.setAdmittedFrom(addmitedFrom.getText());
        // дата заполнения может не меняться

        // Обновлять пациента не требуется, так как ФИО не редактируется

        // Новые значения уже добавлены в allValues, ничего дополнительно делать не нужно
        // Можно лишь очистить множество newValues (по желанию)
        newValues.clear();

        // Сигнал об успешном сохранении
        System.out.println("Данные сохранены в переданные объекты");
    }

    private void handlePredict() {
        List<CharacterizationAnketPatient> latest = getLatestValues();
        List<CharasteristicDTO> dtos = toCharasteristicDTOs(latest);
        PredictionModule predictionModule = PredictionModule.getInstance();
        try {
            PredictionResult result = predictionModule.predict(dtos);
            // Показать результат в модальном окне (здесь не реализовано)
            System.out.println("Результат предсказания: " + result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ------------------- Вспомогательные методы -------------------

    private List<CharasteristicDTO> toCharasteristicDTOs(List<CharacterizationAnketPatient> values) {
        List<CharasteristicDTO> dtos = new ArrayList<>();
        for (CharacterizationAnketPatient cap : values) {
            Characteristic c = characteristicsMap.get(cap.getIdCharacteristic());
            CharasteristicDTO dto = new CharasteristicDTO(c);
            if (optionTexts.containsKey(cap.getIdCharacteristic())) {
                dto.setValue(cap.getIdValue());
            } else {
                dto.setValue(cap.getValue());
            }
            dtos.add(dto);
        }
        return dtos;
    }

    private List<CharacterizationAnketPatient> getLatestValues() {
        List<CharacterizationAnketPatient> result = new ArrayList<>();
        for (List<CharacterizationAnketPatient> list : valuesByCharacteristic.values()) {
            if (!list.isEmpty()) {
                result.add(list.get(0));
            }
        }
        return result;
    }

    private String diagnosisToCode(String diagnosisText) {
        return DIAGNOSIS_MAP.getOrDefault(diagnosisText, "0");
    }

    // Геттеры для внешнего использования (если нужны)
    public Questionnaire getQuestionnaire() { return questionnaire; }
    public Patient getPatient() { return patient; }
    public List<CharacterizationAnketPatient> getAllValues() { return allValues; }
    public Set<CharacterizationAnketPatient> getNewValues() { return new HashSet<>(newValues); }
}
