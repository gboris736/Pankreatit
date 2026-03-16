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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.WindowEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QuestionListTableViewTrainSetControllerOld {

    @FXML private TableView<QuestionnaireItem> tableViewQuestion;
    @FXML private TableColumn<QuestionnaireItem, String> colNamePerson;
    @FXML private TableColumn<QuestionnaireItem, String> colDate;
    @FXML private TableColumn<QuestionnaireItem, String> colDiagnosis;
    @FXML private TableColumn<QuestionnaireItem, Void> colAction;

    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private Button btnSave;       // новая кнопка сохранения
    @FXML private Label lblStatus;      // статус: "Сохранено", "Есть изменения..."

    private final ObservableList<QuestionnaireItem> rows = FXCollections.observableArrayList();
    private FilteredList<QuestionnaireItem> filteredRows;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TrainSetModule trainSetModule = TrainSetModule.getInstance();
    private final DatabaseModule databaseModule = DatabaseModule.getInstance();

    // 🔴 Dirty flag: отслеживаем несохраненные изменения
    private boolean hasUnsavedChanges = false;

    // 🟢 Кэш ID в выборке для быстрого поиска (O(1) вместо O(n))
    private Set<Integer> trainSetIdsCache = new HashSet<>();

    @FXML
    private void initialize() {
        setupTableColumns();
        setupSearch();
        setupRowFactory();
        setupSaveButton();
        setupWindowCloseHandler();
        loadData();
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Настройка колонок таблицы
    // ─────────────────────────────────────────────────────────────
    private void setupTableColumns() {
        colNamePerson.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getFioPatient()));
        colDiagnosis.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDiagnosis()));
        colDate.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDateOfCompletion()));

        // Колонка с кнопками: создаём ячейки один раз, не в updateItem!
        colAction.setCellFactory(param -> new ActionCell());

        HelpUtils.attachHelp(colNamePerson, "ФИО пациента");
        HelpUtils.attachHelp(colDate, "Дата создания анкеты");
        HelpUtils.attachHelp(colDiagnosis, "Поставленный диагноз");
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Кастомная ячейка с кнопками (без утечек памяти)
    // ─────────────────────────────────────────────────────────────
    private static class ActionCell extends TableCell<QuestionnaireItem, Void> {
        private final Button approveBtn = createStyledButton("✅", "approve");
        private final Button rejectBtn = createStyledButton("❌", "reject");
        private final HBox container = new HBox(5, approveBtn, rejectBtn);

        public ActionCell() {
            container.setAlignment(javafx.geometry.Pos.CENTER);
            container.setPadding(new Insets(2));
        }

        private static Button createStyledButton(String text, String type) {
            Button btn = new Button(text);
            btn.setMinWidth(30);
            btn.getStyleClass().addAll("table-button", "table-button-" + type);
            btn.setOnAction(e -> e.consume()); // предотвращаем снятие выделения
            return btn;
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }

            QuestionnaireItem questionnaireItem = getTableRow().getItem();
            Questionnaire quest = DatabaseModule.getInstance()
                    .getQuestionnaireById(questionnaireItem.getIdQuestionnaire());

            // ⚠️ Важно: НЕ создаём новые лямбды каждый раз!
            // Вместо этого используем userData для хранения ссылки
            approveBtn.setUserData(quest);
            rejectBtn.setUserData(quest);

            setGraphic(container);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Окрашивание строк: зеленый = в выборке, красный = нет
    // ─────────────────────────────────────────────────────────────
    private void setupRowFactory() {
        tableViewQuestion.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(QuestionnaireItem item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-in-trainset", "row-not-in-trainset");

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                // 🔍 Быстрая проверка через HashSet (O(1))
                boolean isInTrainSet = trainSetIdsCache.contains(item.getIdQuestionnaire());

                if (isInTrainSet) {
                    setStyle("-fx-background-color: #d4edda;"); // 🟢 зеленый
                    getStyleClass().add("row-in-trainset");
                } else {
                    setStyle("-fx-background-color: #f8d7da;"); // 🔴 красный
                    getStyleClass().add("row-not-in-trainset");
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Поиск по таблице
    // ─────────────────────────────────────────────────────────────
    private void setupSearch() {
        filteredRows = new FilteredList<>(rows, p -> true);
        tableViewQuestion.setItems(filteredRows);

        colDate.setSortType(TableColumn.SortType.DESCENDING);
        tableViewQuestion.getSortOrder().add(colDate);

        if (searchField != null) {
            searchField.setPromptText("Поиск: ФИО пациента или диагноз...");
            searchField.textProperty().addListener((obs, oldV, newV) -> {
                String query = newV == null ? "" : newV.trim().toLowerCase();
                filteredRows.setPredicate(row -> {
                    if (query.isEmpty()) return true;
                    return containsIgnoreCase(row.getFioPatient(), query) ||
                            containsIgnoreCase(row.getDiagnosis(), query);
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
            int size = filteredRows != null ? filteredRows.size() : rows.size();
            countLabel.setText("Найдено: " + size);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 5. Кнопка Save + dirty flag + асинхронное сохранение
    // ─────────────────────────────────────────────────────────────
    private void setupSaveButton() {
        if (btnSave != null) {
            btnSave.setText("💾 Сохранить изменения");
            btnSave.setDisable(true); // изначально неактивна
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
                lblStatus.setStyle("-fx-text-fill: #dc3545;");
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

    // 🔁 Асинхронное сохранение (не блокирует UI)
    @FXML
    private void saveChangesAsync() {
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                updateMessage("Сохранение в базу данных...");
                // Здесь можно добавить реальную запись в БД, если нужно
                // trainSetModule.saveToDatabase();
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            // Обновляем кэш после сохранения
            refreshTrainSetCache();
            tableViewQuestion.refresh(); // перерисовать цвета строк
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
    // 6. Обработчик закрытия окна с подтверждением
    // ─────────────────────────────────────────────────────────────
    private void setupWindowCloseHandler() {
        if (tableViewQuestion != null && tableViewQuestion.getScene() != null) {
            tableViewQuestion.getScene().getWindow().addEventHandler(
                    WindowEvent.WINDOW_CLOSE_REQUEST, this::onWindowCloseRequest);
        }
    }

    private void onWindowCloseRequest(WindowEvent event) {
        if (hasUnsavedChanges) {
            event.consume(); // отменяем закрытие
            showConfirmSaveDialog();
        }
        // если изменений нет — окно закроется автоматически
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
                // Отменить изменения: перезагрузить данные
                rollbackChanges();
                closeWindow();
            } else if (response == btnDiscardAndClose) {
                // Просто закрыть без сохранения
                closeWindow();
            }
            // если btnCancel — ничего не делаем, окно остаётся открытым
        });
    }

    private void rollbackChanges() {
        // Перезагружаем выборку из модуля (отменяем локальные изменения)
        trainSetModule.load();
        refreshTrainSetCache();
        tableViewQuestion.refresh();
        markAsClean();
    }

    private void closeWindow() {
        if (tableViewQuestion != null && tableViewQuestion.getScene() != null) {
            tableViewQuestion.getScene().getWindow().hide();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 7. Загрузка данных и обработка кнопок ✅ / ❌
    // ─────────────────────────────────────────────────────────────
    private void loadData() {
        // Загружаем анкеты в таблицу
        List<QuestionnaireItem> allItems = databaseModule.getAllQuestionnaireItems();
        rows.setAll(allItems);

        // Загружаем выборку и обновляем кэш
        trainSetModule.load();
        refreshTrainSetCache();

        tableViewQuestion.refresh();
        updateCountLabel();
    }

    // 🔄 Обновляем HashSet для быстрого поиска
    private void refreshTrainSetCache() {
        trainSetIdsCache = new HashSet<>(
                trainSetModule.getQuestionIdTrainList()
        );
    }

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
                markAsDirty(); // 🔴 помечаем как изменённое
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
        // Подтверждение перед удалением
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
                        markAsDirty(); // 🔴 помечаем как изменённое
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
        tableViewQuestion.refresh();
        updateCountLabel();
    }

    // ─────────────────────────────────────────────────────────────
    // 8. Вспомогательные методы
    // ─────────────────────────────────────────────────────────────
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
}