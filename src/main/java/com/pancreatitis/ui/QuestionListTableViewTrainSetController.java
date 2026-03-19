package com.pancreatitis.ui;

import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.QuestionnaireItem;
import com.pancreatitis.modules.database.DatabaseModule;
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

    @FXML public TableView<QuestionnaireItemTrainUI> tableViewTrainQuestion;
    @FXML public TableColumn<QuestionnaireItemTrainUI, Void> colSelectTQ;
    @FXML public TableColumn<QuestionnaireItemTrainUI, String> colNamePersonTQ;
    @FXML public TableColumn<QuestionnaireItemTrainUI, String> colDateTQ;
    @FXML public TableColumn<QuestionnaireItemTrainUI, String> colDiagnosisTQ;

    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private Label lblStatus;
    @FXML private Button btnSave;
    @FXML private Button btnSubmit;
    @FXML private Button btnAddToTrain;
    @FXML private Button btnRemoveFromTrain;
    @FXML private HBox legendBox;

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
    private static final String COLOR_SELECTED_FOR_MOVE = "#FFF9C4";   // 🟡 жёлтый: отмечен чекбоксом
    private static final String COLOR_MODIFIED = "#E3F2FD";             // 🔵 голубой: изменён, не сохранён
    private static final String COLOR_IN_TRAINSET = "#E8F5E9";          // 🟢 зелёный: в выборке, без изменений

    @FXML
    private void initialize() {
        setupTableColumns();
        setupSearch();
        setupRowFactory();
        setupSaveButton();
        setupSubmitButton();
        setupWindowCloseHandler();
        setupLegend();
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

        colNamePersonTQ.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().item.getFioPatient()));
        colDiagnosisTQ.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().item.getDiagnosis()));
        colDateTQ.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().item.getDateOfCompletion()));

        colSelectUQ.setCellFactory(param -> new ActionCell());
        colSelectTQ.setCellFactory(param -> new ActionCell());

        colNamePersonUQ.setSortable(true);
        colNamePersonTQ.setSortable(true);
        colDateUQ.setSortable(true);
        colDateTQ.setSortable(true);
        colDiagnosisUQ.setSortable(true);
        colDiagnosisTQ.setSortable(true);
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
        List<QuestionnaireItemTrainUI> selected = fromList.stream()
                .filter(item -> item.move)
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Ничего не выбрано",
                    "Отметьте чекбоксами анкеты для перемещения");
            return;
        }

        for (QuestionnaireItemTrainUI item : selected) {
            fromList.remove(item);
            item.isTrain = newTrainState;
            item.move = false;
            toList.add(item);
            allItemsMap.put(item.item.getIdQuestionnaire(), item);
        }

        toList.sort(Comparator.comparing(
                (QuestionnaireItemTrainUI i) -> i.item.getDateOfCompletion(),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        markAsDirty();
        refreshTables();
        System.out.println("✅ Перемещено: " + selected.size() + " элементов");
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
            checkBox.setSelected(data.move);
            setGraphic(checkBox);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Фабрика строк с трёхцветной подсветкой (старый вариант)
    // ─────────────────────────────────────────────────────────────
    private void setupRowFactory() {
        tableViewUsualQuestion.setRowFactory(tv -> new TableRow<QuestionnaireItemTrainUI>() {
            @Override
            protected void updateItem(QuestionnaireItemTrainUI item, boolean empty) {
                super.updateItem(item, empty);

                // Двойной клик: быстро отметить/снять отметку
                setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !isEmpty()) {
                        QuestionnaireItemTrainUI data = getItem();
                        if (data != null) {
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
                        if (data != null) {
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
     * Приоритет: 1) отмечен чекбоксом → 2) изменён → 3) в выборке → 4) обычный
     */
    private static String getRowStyleForItem(QuestionnaireItemTrainUI item) {
        if (item == null) return "";

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
    // 5. Легенда цветов
    // ─────────────────────────────────────────────────────────────
    private void setupLegend() {
        if (legendBox != null) {
            legendBox.setSpacing(8);
            legendBox.setPadding(new Insets(5, 0, 0, 0));
            legendBox.getChildren().addAll(
                    createLegendItem("🟡 Отмечен", COLOR_SELECTED_FOR_MOVE),
                    createLegendItem("🔵 Изменён", COLOR_MODIFIED),
                    createLegendItem("🟢 В выборке", COLOR_IN_TRAINSET)
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
            btnSave.setOnAction(e -> saveChangesAsync());
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

    private void saveTrainChangeInFile(List<QuestionnaireItemTrainUI> itemsList) {

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                updateMessage("Добавление в выборку...");

                for (QuestionnaireItemTrainUI item : itemsList) {
                    Questionnaire questionnaire = databaseModule.getQuestionnaireById(item.item.getIdQuestionnaire());

                    if (item.isTrain) {
                        trainSetModule.addQuestionnaire(
                                questionnaire,
                                getLatestCharacterizationsForAnket((int) questionnaire.getId())
                        );
                    }
                    else {
                        trainSetModule.deleteQuestionnaire(questionnaire);
                    }
                }

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
    private void saveChangesAsync() {
        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() {
                updateMessage("Добавление в выборку...");

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

                trainSetModule.saveChanges();

                return true;
            }
        };
        saveTask.setOnSucceeded(e -> {
            refreshTrainSetCache();
            for (QuestionnaireItemTrainUI ui : allItemsMap.values()) {
                ui.originalIsTrain = ui.isTrain;
            }
            tableViewUsualQuestion.refresh();
            tableViewTrainQuestion.refresh();
            markAsClean();
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
            btnSubmit.setOnAction(e -> submitChangesAsync());
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
        // if (tableViewUsualQuestion != null && tableViewUsualQuestion.getScene() != null) {
        //     tableViewUsualQuestion.getScene().getWindow().addEventHandler(
        //             WindowEvent.WINDOW_CLOSE_REQUEST, this::onWindowCloseRequest);
        // }
    }

    private void onWindowCloseRequest(WindowEvent event) {
        if (hasUnsavedChanges) {
            event.consume();
            showConfirmSaveDialog();
        }
    }

    private void showConfirmSaveDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Несохраненные изменения");
        alert.setHeaderText("Есть несохраненные изменения в выборке");
        alert.setContentText("Что вы хотите сделать?");

        ButtonType btnDiscard = new ButtonType("Отменить изменения", ButtonBar.ButtonData.NO);
        ButtonType btnDiscardAndClose = new ButtonType("Не сохранять и выйти", ButtonBar.ButtonData.NO);
        ButtonType btnCancel = new ButtonType("Остаться", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnDiscard, btnDiscardAndClose, btnCancel);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnDiscard) {
                resetChanges();
                closeWindow();
            } else if (response == btnDiscardAndClose) {
                closeWindow();
            }
        });
    }

    private void resetChanges() {
        for (QuestionnaireItemTrainUI ui : allItemsMap.values()) {
            ui.isTrain = ui.originalIsTrain;
            ui.move = false;
        }
        reloadDataFromModules();
        markAsClean();
    }

    private void closeWindow() {
        // if (tableViewUsualQuestion != null && tableViewUsualQuestion.getScene() != null) {
        //     tableViewUsualQuestion.getScene().getWindow().hide();
        // }
    }

    // ─────────────────────────────────────────────────────────────
    // 8. Загрузка данных
    // ─────────────────────────────────────────────────────────────
    private void loadData() {
        allItems = databaseModule.getAllQuestionnaireItems();
        trainSetModule.load();
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

        tableViewTrainQuestion.refresh();
        tableViewUsualQuestion.refresh();
        updateCountLabel();
        markAsClean();
    }

    private void reloadDataFromModules() {
        trainSetModule.load();
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
        trainSetModule.load();
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