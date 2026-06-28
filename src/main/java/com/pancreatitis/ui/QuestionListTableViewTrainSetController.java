package com.pancreatitis.ui;

import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.QuestionnaireItem;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;
import com.pancreatitis.modules.trainset.TrainSetModule;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.WindowEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class QuestionListTableViewTrainSetController {

    @FXML public TableView<QuestionnaireItemTrainUI> tableViewUsualQuestion;
    @FXML public TableColumn<QuestionnaireItemTrainUI, Void> colSelectUQ;
    @FXML public TableColumn<QuestionnaireItemTrainUI, String> colDiagnosisUQ;
    @FXML public TableColumn<QuestionnaireItemTrainUI, String> colDateUQ;
    @FXML public TableColumn<QuestionnaireItemTrainUI, String> colNamePersonUQ;
    @FXML public TableColumn<QuestionnaireItemTrainUI, String> colDoctorUQ;

    @FXML public TableView<QuestionnaireItemTrainUI> tableViewTrainQuestion;
    @FXML public TableColumn<QuestionnaireItemTrainUI, Void> colSelectTQ;
    @FXML public TableColumn<QuestionnaireItemTrainUI, String> colNamePersonTQ;
    @FXML public TableColumn<QuestionnaireItemTrainUI, String> colDateTQ;
    @FXML public TableColumn<QuestionnaireItemTrainUI, String> colDiagnosisTQ;
    @FXML public TableColumn<QuestionnaireItemTrainUI, String> colDoctorTQ;

    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private Label lblStatus;
    @FXML private Button btnSave;
    @FXML private Button btnSubmit;
    @FXML private HBox legendBox;
    @FXML private ComboBox<String> cmbAlgorithmFile;

    private final ObservableList<QuestionnaireItemTrainUI> rowsUQ = FXCollections.observableArrayList();
    private final ObservableList<QuestionnaireItemTrainUI> rowsTQ = FXCollections.observableArrayList();
    private FilteredList<QuestionnaireItemTrainUI> filteredRowsUQ;
    private FilteredList<QuestionnaireItemTrainUI> filteredRowsTQ;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TrainSetModule trainSetModule = TrainSetModule.getInstance();
    private final DatabaseModule databaseModule = DatabaseModule.getInstance();

    private boolean hasUnsavedChanges = false;
    private Set<Integer> trainSetIdsCache = new HashSet<>();
    private List<QuestionnaireItem> allItems;
    private final Map<Integer, QuestionnaireItemTrainUI> allItemsMap = new HashMap<>();

    // 🎨 Цвета для состояний
    private static final String COLOR_SELECTED_FOR_MOVE = "#fbf2bc";   // 🟡 жёлтый: отмечен чекбоксом
    private static final String COLOR_MODIFIED = "#badef7";             // 🔵 голубой: изменён, не сохранён
    private static final String COLOR_IN_TRAINSET = "#c1f5c5";          // 🟢 зелёный: в выборке, без изменений
    // CHANGE: добавлен оранжевый для анкет без диагноза
    private static final String COLOR_NO_DIAGNOSIS = "#FFB74D";         // 🟠 оранжевый: диагноз отсутствует

    @FXML
    private void initialize() {
        setupTableColumns();
        setupSearch();
        setupRowFactory();
        setupSaveButton();
        setupSubmitButton();
        setupWindowCloseHandler();
        setupLegend();      // CHANGE: легенда теперь включает оранжевый
        setupAlgorithmFileCombo();
        loadData();
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Настройка колонок таблицы
    // ─────────────────────────────────────────────────────────────
    private void setupTableColumns() {
        colNamePersonUQ.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().item.getFioPatient()));
        colDiagnosisUQ.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().item.getDiagnosis()));
        colDateUQ.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().item.getDateOfCompletion()));
        colDoctorUQ.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().item.getFioDoctor()));

        colNamePersonTQ.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().item.getFioPatient()));
        colDiagnosisTQ.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().item.getDiagnosis()));
        colDateTQ.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().item.getDateOfCompletion()));
        colDoctorTQ.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().item.getFioDoctor()));

        colSelectUQ.setCellFactory(param -> new ActionCell());
        colSelectTQ.setCellFactory(param -> new ActionCell());

        colNamePersonUQ.setSortable(true);
        colNamePersonTQ.setSortable(true);
        colDateUQ.setSortable(true);
        colDateTQ.setSortable(true);
        colDiagnosisUQ.setSortable(true);
        colDiagnosisTQ.setSortable(true);
        colDoctorUQ.setSortable(true);
        colDoctorTQ.setSortable(true);
    }

    private Comparator<QuestionnaireItemTrainUI> getModifiedFirstComparator() {
        return Comparator
                .comparing((QuestionnaireItemTrainUI i) -> !i.isModified()) // false (изменённые) идут первыми
                .thenComparing(i -> i.item.getDateOfCompletion(),
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }

    @FXML
    private void handleMoveToTrain() {
        moveSelectedItems(rowsUQ, rowsTQ, true);
    }

    @FXML
    private void handleMoveToUsual() {
        moveSelectedItems(rowsTQ, rowsUQ, false);
    }

    private void moveSelectedItems(ObservableList<QuestionnaireItemTrainUI> fromList,
                                   ObservableList<QuestionnaireItemTrainUI> toList,
                                   boolean newTrainState) {
        // CHANGE: исключаем элементы без диагноза (они не имеют чекбокса, но на всякий случай фильтруем)
        List<QuestionnaireItemTrainUI> selected = fromList.stream()
                .filter(item -> item.move && !isNoDiagnosis(item))
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Ничего не выбрано",
                    "Отметьте чекбоксами анкеты для перемещения (анкеты без диагноза недоступны)");
            return;
        }

        for (QuestionnaireItemTrainUI item : selected) {
            fromList.remove(item);
            item.isTrain = newTrainState;
            item.move = false;
            toList.add(item);
            allItemsMap.put(item.item.getIdQuestionnaire(), item);
        }

        FXCollections.sort(toList, getModifiedFirstComparator());

        markAsDirty();
        refreshTables();
    }

    private void refreshTables() {
        if (filteredRowsUQ != null) filteredRowsUQ.setPredicate(filteredRowsUQ.getPredicate());
        if (filteredRowsTQ != null) filteredRowsTQ.setPredicate(filteredRowsTQ.getPredicate());
        tableViewUsualQuestion.refresh();
        tableViewTrainQuestion.refresh();
        updateCountLabel();
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Кастомная ячейка с чекбоксом
    // ─────────────────────────────────────────────────────────────
    private static class ActionCell extends TableCell<QuestionnaireItemTrainUI, Void> {
        private final CheckBox checkBox = new CheckBox();

        public ActionCell() {
            checkBox.setAlignment(Pos.CENTER);
            checkBox.setPadding(new Insets(2));
            checkBox.setOnAction(e -> {
                QuestionnaireItemTrainUI item = getTableRow().getItem();
                if (item != null && !isEmpty()) {
                    item.move = checkBox.isSelected();
                    if (getTableRow() != null) {
                        getTableRow().setStyle(getRowStyleForItem(item));
                    }
                }
                e.consume();
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            QuestionnaireItemTrainUI data = getTableRow().getItem();
            // CHANGE: если диагноз отсутствует, чекбокс не показываем
            if (isNoDiagnosis(data)) {
                setGraphic(null);
                return;
            }
            checkBox.setSelected(data.move);
            setGraphic(checkBox);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Фабрика строк с подсветкой + двойной клик
    // ─────────────────────────────────────────────────────────────
    private void setupRowFactory() {
        tableViewUsualQuestion.setRowFactory(tv -> new TableRow<QuestionnaireItemTrainUI>() {
            @Override
            protected void updateItem(QuestionnaireItemTrainUI item, boolean empty) {
                super.updateItem(item, empty);

                setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !isEmpty()) {
                        QuestionnaireItemTrainUI data = getItem();
                        // CHANGE: не реагируем на двойной клик, если диагноз отсутствует
                        if (data != null && !isNoDiagnosis(data)) {
                            data.move = !data.move;
                            setStyle(getRowStyleForItem(data));
                            tableViewUsualQuestion.refresh();
                        }
                        event.consume();
                    }
                });

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                setStyle(getRowStyleForItem(item));
            }
        });

        tableViewTrainQuestion.setRowFactory(tv -> new TableRow<QuestionnaireItemTrainUI>() {
            @Override
            protected void updateItem(QuestionnaireItemTrainUI item, boolean empty) {
                super.updateItem(item, empty);

                setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !isEmpty()) {
                        QuestionnaireItemTrainUI data = getItem();
                        if (data != null && !isNoDiagnosis(data)) {
                            data.move = !data.move;
                            setStyle(getRowStyleForItem(data));
                            tableViewTrainQuestion.refresh();
                        }
                        event.consume();
                    }
                });

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                setStyle(getRowStyleForItem(item));
            }
        });
    }

    /**
     * Возвращает CSS-стиль для строки на основе состояния элемента
     * Приоритет: 0) без диагноза (оранжевый) → 1) отмечен чекбоксом → 2) изменён → 3) в выборке → 4) обычный
     */
    private static String getRowStyleForItem(QuestionnaireItemTrainUI item) {
        if (item == null) return "";

        // CHANGE: 🟠 Приоритет 0: диагноз отсутствует – блокируем, красим в оранжевый
        if (isNoDiagnosis(item)) {
            return "-fx-background-color: " + COLOR_NO_DIAGNOSIS + "; -fx-opacity: 0.8;";
        }

        // 🟡 Приоритет 1: отмечен чекбоксом для перемещения
        if (item.move) {
            return "-fx-background-color: " + COLOR_SELECTED_FOR_MOVE + "; -fx-font-weight: bold;";
        }

        // 🔵 Приоритет 2: состояние изменено (перемещён), но чекбокс снят
        if (item.isTrain != item.originalIsTrain) {
            return "-fx-background-color: " + COLOR_MODIFIED + "; -fx-font-style: italic;";
        }

        // 🟢 Приоритет 3: элемент в обучающей выборке, без изменений
        if (item.isTrain) {
            return "-fx-background-color: " + COLOR_IN_TRAINSET + ";";
        }

        // ⚪ Обычное состояние
        return "";
    }

    // CHANGE: вспомогательный метод проверки отсутствия диагноза
    private static boolean isNoDiagnosis(QuestionnaireItemTrainUI item) {
        if (item == null || item.item == null) return true;
        String d = item.item.getDiagnosis();
        return d == null || d.trim().isEmpty() || d.equals("-");
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Поиск по таблице
    // ─────────────────────────────────────────────────────────────
    private void setupSearch() {
        filteredRowsUQ = new FilteredList<>(rowsUQ, p -> true);
        filteredRowsTQ = new FilteredList<>(rowsTQ, p -> true);

        tableViewUsualQuestion.setItems(filteredRowsUQ);
        tableViewTrainQuestion.setItems(filteredRowsTQ);

        colDateTQ.setSortType(TableColumn.SortType.DESCENDING);
        colDateUQ.setSortType(TableColumn.SortType.DESCENDING);
        tableViewTrainQuestion.getSortOrder().add(colDateTQ);
        tableViewUsualQuestion.getSortOrder().add(colDateUQ);

        FXCollections.sort(rowsUQ, getModifiedFirstComparator());
        FXCollections.sort(rowsTQ, getModifiedFirstComparator());

        if (searchField != null) {
            searchField.setPromptText("Поиск: ФИО пациента или диагноз...");
            searchField.textProperty().addListener((obs, oldV, newV) -> {
                String query = newV == null ? "" : newV.trim().toLowerCase();

                filteredRowsUQ.setPredicate(row -> {
                    if (query.isEmpty()) return true;
                    return containsIgnoreCase(row.item.getFioPatient(), query) ||
                            containsIgnoreCase(row.item.getDiagnosis(), query);
                });
                filteredRowsTQ.setPredicate(row -> {
                    if (query.isEmpty()) return true;
                    return containsIgnoreCase(row.item.getFioPatient(), query) ||
                            containsIgnoreCase(row.item.getDiagnosis(), query);
                });

                updateCountLabel();
            });
        }
        updateCountLabel();
    }

    private boolean containsIgnoreCase(String source, String query) {
        return source != null && source.toLowerCase().contains(query);
    }

    private void updateCountLabel() {
        if (countLabel != null) {
            int sizeTQ = filteredRowsTQ != null ? filteredRowsTQ.size() : rowsTQ.size();
            int sizeUQ = filteredRowsUQ != null ? filteredRowsUQ.size() : rowsUQ.size();
            countLabel.setText("Найдено: " + (sizeTQ + sizeUQ) + " (в выборке: " + sizeTQ + ")");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 5. Легенда цветов (добавлен оранжевый)
    // ─────────────────────────────────────────────────────────────
    private void setupLegend() {
        if (legendBox != null) {
            legendBox.setSpacing(8);
            legendBox.setPadding(new Insets(5, 0, 0, 0));
            legendBox.getChildren().addAll(
                    createLegendItem("🟡 Отмечен", COLOR_SELECTED_FOR_MOVE),
                    createLegendItem("🔵 Изменён", COLOR_MODIFIED),
                    createLegendItem("🟢 В выборке", COLOR_IN_TRAINSET),
                    createLegendItem("🟠 Без диагноза", COLOR_NO_DIAGNOSIS)   // CHANGE
            );
        }
    }

    private Label createLegendItem(String text, String color) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: " + color + "; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");
        return label;
    }

    // ─────────────────────────────────────────────────────────────
    // 6. Кнопка Save + dirty flag + асинхронное сохранение
    // ─────────────────────────────────────────────────────────────
    private void setupSaveButton() {
        if (btnSave != null) {
            btnSave.setText("💾 Сохранить изменения");
            btnSave.setDisable(true);
            btnSave.setOnAction(e -> {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Подтверждение сохранения");
                confirmAlert.setHeaderText("Вы уверены, что хотите сохранить изменения в обучающей выборке?");
                ButtonType result = confirmAlert.showAndWait().orElse(ButtonType.CANCEL);
                if (result != ButtonType.OK) {
                    return;
                }
                saveChangesAsync();
            });
        }
        updateSaveButtonState();
    }

    private void markAsDirty() {
        if (!hasUnsavedChanges) {
            hasUnsavedChanges = true;
            updateSaveButtonState();
            if (lblStatus != null) {
                lblStatus.setText("⚠️ Есть несохраненные изменения");
                lblStatus.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
            }
        }
    }

    private void markAsClean() {
        hasUnsavedChanges = false;
        updateSaveButtonState();
        if (lblStatus != null) {
            lblStatus.setText("✓ Все изменения сохранены");
            lblStatus.setStyle("-fx-text-fill: #28a745;");
        }
    }

    private void updateSaveButtonState() {
        if (btnSave != null) {
            btnSave.setDisable(!hasUnsavedChanges);
        }
    }

    @FXML
    private void saveChangesAsync() {
        Task<String> saveTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                updateMessage("Сохранение изменений...");

                for (QuestionnaireItem item : getAddedToTrain()) {
                    Questionnaire questionnaire = databaseModule.getQuestionnaireById(item.getIdQuestionnaire());
                    trainSetModule.addQuestionnaire(
                            questionnaire,
                            getLatestCharacterizationsForAnket((int) questionnaire.getId())
                    );
                }

                for (QuestionnaireItem item : getRemovedFromTrain()) {
                    Questionnaire questionnaire = databaseModule.getQuestionnaireById(item.getIdQuestionnaire());
                    trainSetModule.deleteQuestionnaire(questionnaire);
                }

                boolean saved = trainSetModule.saveChanges();
                if (!saved) {
                    throw new RuntimeException("Не удалось записать файл.");
                }

                trainSetModule.enforceMaxFiles(5);

                return trainSetModule.getCurrentFileName();
            }
        };

        saveTask.setOnSucceeded(e -> {
            String actualFileName = saveTask.getValue();

            refreshTrainSetCache();
            for (QuestionnaireItemTrainUI ui : allItemsMap.values()) {
                ui.originalIsTrain = ui.isTrain;
            }
            tableViewUsualQuestion.refresh();
            tableViewTrainQuestion.refresh();
            markAsClean();

            if (cmbAlgorithmFile != null) {
                List<String> updatedFiles = LocalStorageModule.getInstance().listAlgorithmFiles();
                cmbAlgorithmFile.getItems().setAll(updatedFiles);
                if (actualFileName != null) {
                    cmbAlgorithmFile.setValue(actualFileName);
                }
            }

            showAlert(Alert.AlertType.INFORMATION, "Успех", "Изменения успешно сохранены!");
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            showAlert(Alert.AlertType.ERROR, "Ошибка сохранения",
                    "Не удалось сохранить изменения:\n" + ex.getMessage());
            ex.printStackTrace();
        });

        new Thread(saveTask).start();
    }

    // ─────────────────────────────────────────────────────────────
    // 6.5. Кнопка Submit + асинхронная отправка
    // ─────────────────────────────────────────────────────────────
    private void setupSubmitButton() {
        if (btnSubmit != null) {
            btnSubmit.setText("💾 Отправить изменения");
            btnSubmit.setOnAction(e -> {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Подтверждение отправки");
                confirmAlert.setHeaderText("Вы уверены, что хотите отправить новую версию на диск?");
                confirmAlert.setContentText("Это действие опубликует текущие изменения");
                ButtonType result = confirmAlert.showAndWait().orElse(ButtonType.CANCEL);
                if (result != ButtonType.OK) {
                    return;
                }
                submitChangesAsync();
            });
        }
    }

    @FXML
    private void submitChangesAsync() {
        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() {
                updateMessage("Отправка новой версии...");
                trainSetModule.submit();
                return true;
            }
        };

        saveTask.setOnSucceeded(e -> {
            showAlert(Alert.AlertType.INFORMATION, "Успех", "Новая версия успешно отправлена!");
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            showAlert(Alert.AlertType.ERROR, "Ошибка отправка",
                    "Не удалось отправить новую версию:\n" + ex.getMessage());
            ex.printStackTrace();
        });

        new Thread(saveTask).start();
    }

    // ─────────────────────────────────────────────────────────────
    // 7. Обработчик закрытия окна
    // ─────────────────────────────────────────────────────────────
    private void setupWindowCloseHandler() {
        // (оставлено как есть)
    }

    private void resetChanges() {
        for (QuestionnaireItemTrainUI ui : allItemsMap.values()) {
            ui.isTrain = ui.originalIsTrain;
            ui.move = false;
        }
        reloadDataFromModules();
        markAsClean();
    }

    // ─────────────────────────────────────────────────────────────
    // 8. Загрузка данных
    // ─────────────────────────────────────────────────────────────
    private void setupAlgorithmFileCombo() {
        if (cmbAlgorithmFile == null) return;

        List<String> files = LocalStorageModule.getInstance().listAlgorithmFiles();
        cmbAlgorithmFile.getItems().setAll(files);

        String first = TrainSetModule.getInstance().getLatestAlgorithmFileName();
        cmbAlgorithmFile.setValue(first);
        trainSetModule.loadFromFile(first);
        reloadDataFromModules();

        cmbAlgorithmFile.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.equals(oldVal)) return;

            if (hasUnsavedChanges) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Несохранённые изменения");
                confirm.setHeaderText("Есть несохранённые изменения. Сохранить перед загрузкой другой версии?");
                confirm.setContentText("Выберите действие.");

                ButtonType discardBtn = new ButtonType("Отменить изменения и загрузить");
                ButtonType cancelBtn = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirm.getButtonTypes().setAll(discardBtn, cancelBtn);
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == discardBtn) {
                        resetChanges();
                        loadSelectedAlgorithmFile(newVal);
                    }
                }
            } else {
                loadSelectedAlgorithmFile(newVal);
            }
        });
    }

    private void loadSelectedAlgorithmFile(String fileName) {
        boolean ok = trainSetModule.loadFromFile(fileName);
        if (ok) {
            reloadDataFromModules();
            lblStatus.setText("✓ Загружена версия: " + fileName);
            lblStatus.setStyle("-fx-text-fill: #28a745;");
        } else {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить файл " + fileName);
        }
    }

    private void loadData() {
        allItems = databaseModule.getAllQuestionnaireItems();
        refreshTrainSetCache();

        rowsUQ.clear();
        rowsTQ.clear();
        allItemsMap.clear();

        for (QuestionnaireItem item : allItems) {
            boolean initiallyInTrain = trainSetIdsCache.contains(item.getIdQuestionnaire());
            QuestionnaireItemTrainUI ui = new QuestionnaireItemTrainUI(item, initiallyInTrain);
            allItemsMap.put(item.getIdQuestionnaire(), ui);

            if (initiallyInTrain) {
                rowsTQ.add(ui);
            } else {
                rowsUQ.add(ui);
            }
        }

        FXCollections.sort(rowsUQ, getModifiedFirstComparator());
        FXCollections.sort(rowsTQ, getModifiedFirstComparator());

        tableViewTrainQuestion.refresh();
        tableViewUsualQuestion.refresh();
        updateCountLabel();
        markAsClean();
    }

    private void reloadDataFromModules() {
        refreshTrainSetCache();

        for (QuestionnaireItemTrainUI ui : allItemsMap.values()) {
            boolean newTrainState = trainSetIdsCache.contains(ui.item.getIdQuestionnaire());
            ui.isTrain = newTrainState;
            ui.originalIsTrain = newTrainState;
            ui.move = false;
        }

        rowsUQ.clear();
        rowsTQ.clear();
        for (QuestionnaireItemTrainUI ui : allItemsMap.values()) {
            if (ui.isTrain) rowsTQ.add(ui);
            else rowsUQ.add(ui);
        }

        FXCollections.sort(rowsUQ, getModifiedFirstComparator());
        FXCollections.sort(rowsTQ, getModifiedFirstComparator());

        refreshTables();
    }

    private void refreshTrainSetCache() {
        trainSetIdsCache = new HashSet<>(trainSetModule.getQuestionIdTrainList());
    }

    // ─────────────────────────────────────────────────────────────
    // 9. Методы для получения списков изменённых объектов
    // ─────────────────────────────────────────────────────────────

    public List<QuestionnaireItem> getAddedToTrain() {
        return allItemsMap.values().stream()
                .filter(ui -> ui.isTrain && !ui.originalIsTrain)
                .map(ui -> ui.item)
                .collect(Collectors.toList());
    }

    public List<QuestionnaireItem> getRemovedFromTrain() {
        return allItemsMap.values().stream()
                .filter(ui -> !ui.isTrain && ui.originalIsTrain)
                .map(ui -> ui.item)
                .collect(Collectors.toList());
    }

    public List<QuestionnaireItem> getMovedItems() {
        return allItemsMap.values().stream()
                .filter(ui -> ui.isTrain != ui.originalIsTrain)
                .map(ui -> ui.item)
                .collect(Collectors.toList());
    }

    public boolean hasChanges() {
        return allItemsMap.values().stream()
                .anyMatch(ui -> ui.isTrain != ui.originalIsTrain);
    }

    public void resetChangesPublic() {
        resetChanges();
    }

    // ─────────────────────────────────────────────────────────────
    // 10. Вспомогательные методы
    // ─────────────────────────────────────────────────────────────
    @FXML
    private void handleAdd(Questionnaire questionnaire) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                updateMessage("Добавление в выборку...");
                trainSetModule.addQuestionnaire(
                        questionnaire,
                        getLatestCharacterizationsForAnket((int) questionnaire.getId())
                );
                return true;
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                markAsDirty();
                refreshAfterChange();
            }
        });
        task.setOnFailed(e ->
                showAlert(Alert.AlertType.ERROR, "Ошибка",
                        "Не удалось добавить анкету: " + task.getException().getMessage())
        );
        new Thread(task).start();
    }

    @FXML
    private void handleRemove(Questionnaire questionnaire) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение удаления");
        confirm.setHeaderText("Удалить анкету из обучающей выборки?");
        Patient questPatient = databaseModule.getPatientById((int) questionnaire.getIdPatient());
        confirm.setContentText("Анкета: " + questPatient.getFio());

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        updateMessage("Удаление из выборки...");
                        trainSetModule.deleteQuestionnaire(questionnaire);
                        return true;
                    }
                };

                task.setOnSucceeded(e -> {
                    if (task.getValue()) {
                        markAsDirty();
                        refreshAfterChange();
                    }
                });
                task.setOnFailed(e ->
                        showAlert(Alert.AlertType.ERROR, "Ошибка",
                                "Не удалось удалить анкету: " + task.getException().getMessage())
                );
                new Thread(task).start();
            }
        });
    }

    private void refreshAfterChange() {
        refreshTrainSetCache();
        tableViewTrainQuestion.refresh();
        tableViewUsualQuestion.refresh();
        updateCountLabel();
    }

    public List<CharacterizationAnketPatient> getLatestCharacterizationsForAnket(int anketId) {
        List<CharacterizationAnketPatient> list =
                databaseModule.getCharacterizationsForAnket(anketId);

        return list.stream()
                .collect(Collectors.toMap(
                        CharacterizationAnketPatient::getIdCharacteristic,
                        item -> item,
                        (existing, replacement) -> {
                            try {
                                LocalDateTime exTime = LocalDateTime.parse(existing.getCreatedAt(), formatter);
                                LocalDateTime repTime = LocalDateTime.parse(replacement.getCreatedAt(), formatter);
                                return repTime.isAfter(exTime) ? replacement : existing;
                            } catch (Exception e) {
                                return existing;
                            }
                        }
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────
    // ВНУТРЕННИЙ КЛАСС: хранит исходное и текущее состояние
    // ─────────────────────────────────────────────────────────────
    private static class QuestionnaireItemTrainUI {
        public boolean isTrain;
        public boolean originalIsTrain;
        public boolean move;
        public QuestionnaireItem item;

        public QuestionnaireItemTrainUI(QuestionnaireItem item, boolean initiallyInTrain) {
            this.item = item;
            this.isTrain = initiallyInTrain;
            this.originalIsTrain = initiallyInTrain;
            this.move = false;
        }

        public boolean isModified() {
            return isTrain != originalIsTrain;
        }

        public boolean isSelectedForMove() {
            return move;
        }
    }
}