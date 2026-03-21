// QuestionRequestsList.java (модифицированная версия)
package com.pancreatitis.ui;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.questionnairemanager.QuestionnaireManagerModule;
import com.pancreatitis.modules.updates.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestionRequestsList {

    @FXML
    private Button btnBack;
    @FXML
    private Button btnConfirmAll;
    @FXML
    private Button btnRefresh;
    @FXML
    private Label lblCount;
    @FXML
    private VBox questionnairesContainer;
    @FXML
    private HBox loadingBox;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label lblLoadingStatus;

    private UpdatesModule updatesModule = UpdatesModule.getInstance();
    private boolean isLoading = false;

    // Хранилище для временных данных при асинхронной загрузке
    private final Map<Integer, TempUpdateData> tempUpdateData = new ConcurrentHashMap<>();
    private int totalUpdates = 0;
    private int loadedUpdates = 0;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        loadQuestionnaires();

        btnBack.setOnAction(event -> {
            MainMenuControl mainMenuControl = MainMenuControl.getInstance();
            mainMenuControl.showViewForTab("Главное меню");
        });

        btnRefresh.setOnAction(event -> {
            if (!isLoading) {
                loadQuestionnaires();
            }
        });

        btnConfirmAll.setOnAction(event -> confirmAllQuestionnaires());
    }

    /**
     * Загрузка анкет на верификацию (асинхронная)
     */
    private void loadQuestionnaires() {
        if (isLoading) {
            return;
        }

        // Очищаем контейнер
        questionnairesContainer.getChildren().clear();
        tempUpdateData.clear();

        // Показываем индикатор загрузки
        showLoading(true, "Загрузка списка обновлений...");

        isLoading = true;
        totalUpdates = 0;
        loadedUpdates = 0;

        // Блокируем кнопки во время загрузки
        btnRefresh.setDisable(true);
        btnConfirmAll.setDisable(true);

        // Запускаем асинхронную загрузку
        updatesModule.loadAsync(new UpdateLoadCallback() {
            @Override
            public void onStart(int totalCount) {
                Platform.runLater(() -> {
                    totalUpdates = totalCount;
                    lblLoadingStatus.setText("Найдено обновлений: " + totalCount);
                    if (totalCount == 0) {
                        showLoading(false, "");
                        showEmptyMessage();
                        isLoading = false;
                        btnRefresh.setDisable(false);
                        btnConfirmAll.setDisable(false);
                    }
                });
            }

            @Override
            public void onProgress(int loaded, int total) {
                Platform.runLater(() -> {
                    loadedUpdates = loaded;
                    lblLoadingStatus.setText("Загружено: " + loaded + " из " + total);
                    lblCount.setText("Загружено: " + loaded + " из " + total);
                });
            }

            @Override
            public void onUpdateLoaded(UpdateLoadResult result) {
                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        // Сохраняем данные
                        tempUpdateData.put(result.getIndex(), new TempUpdateData(
                                result.getPatient(),
                                result.getQuestionnaire(),
                                result.getCharacteristics()
                        ));

                        // Создаем карточку
                        createQuestionnaireCard(
                                result.getIndex(),
                                result.getQuestionnaire(),
                                result.getPatient(),
                                result.getCharacteristics()
                        );
                    } else {
                        // Показываем ошибку, но продолжаем
                        showNotification("Ошибка загрузки обновления: " + result.getErrorMessage(), false);
                    }
                });
            }

            @Override
            public void onComplete(int successCount, int failCount) {
                Platform.runLater(() -> {
                    isLoading = false;
                    showLoading(false, "");

                    // Обновляем финальный счетчик
                    lblCount.setText("Найдено: " + successCount);

                    if (successCount == 0) {
                        showEmptyMessage();
                    }

                    // Разблокируем кнопки
                    btnRefresh.setDisable(false);
                    btnConfirmAll.setDisable(false);

                    // Показываем итоговое сообщение
                    if (failCount > 0) {
                        showNotification("Загрузка завершена. Успешно: " + successCount +
                                ", с ошибками: " + failCount, successCount > 0);
                    } else if (successCount > 0) {
                        showNotification("Все обновления успешно загружены (" + successCount + ")", true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    isLoading = false;
                    showLoading(false, "");
                    showNotification(error, false);
                    btnRefresh.setDisable(false);
                    btnConfirmAll.setDisable(false);
                    showEmptyMessage();
                });
            }
        });
    }

    /**
     * Показ пустого сообщения
     */
    private void showEmptyMessage() {
        Label emptyLabel = new Label("Нет анкет на верификацию");
        emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
        emptyLabel.setAlignment(Pos.CENTER);
        emptyLabel.setPrefWidth(Double.MAX_VALUE);
        questionnairesContainer.getChildren().add(emptyLabel);
    }

    /**
     * Показ/скрытие индикатора загрузки
     */
    private void showLoading(boolean show, String message) {
        loadingBox.setVisible(show);
        loadingBox.setManaged(show);
        if (show && message != null) {
            lblLoadingStatus.setText(message);
        }
    }

    /**
     * Создание карточки анкеты
     */
    private void createQuestionnaireCard(int id, Questionnaire questionnaire, Patient patient,
                                         List<CharacterizationAnketPatient> characteristics) {
        // Основной блок карточки
        VBox cardBox = new VBox(10);
        cardBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; " +
                "-fx-background-radius: 5; -fx-padding: 15;" +
                "-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        cardBox.setUserData(id); // Сохраняем ID для быстрого доступа

        // Заголовок с ID и датой
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("Анкета #" + (id + 1));
        idLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label dateLabel = new Label(questionnaire.getDateOfCompletion());
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label("На проверке");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f39c12; " +
                "-fx-background-color: #fef5e7; " +
                "-fx-padding: 5 10 5 10; -fx-background-radius: 10;");

        headerBox.getChildren().addAll(idLabel, dateLabel, spacer, statusLabel);

        // Основная информация о пациенте
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(10);

        infoGrid.getColumnConstraints().addAll(
                createColumnConstraint(150),
                createColumnConstraint(300)
        );

        // ФИО пациента
        addInfoRow(infoGrid, 0, "ФИО пациента:", patient.getFio());

        // Откуда поступил
        addInfoRow(infoGrid, 1, "Откуда поступил:", questionnaire.getAdmittedFrom());

        // Диагноз
        addInfoRow(infoGrid, 2, "Диагноз:", questionnaire.getTextDiagnosis());

        // Количество заполненных характеристик
        int characteristicsCount = characteristics != null ? characteristics.size() : 0;
        addInfoRow(infoGrid, 3, "Заполнено характеристик:", String.valueOf(characteristicsCount));

        // Кнопки верификации
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));

        // Кнопка "Просмотр"
        Button btnView = new Button("📋 Просмотр");
        btnView.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnView.setPrefWidth(120);
        btnView.setOnAction(event -> viewQuestionnaire(id));

        // Кнопка "Подтвердить" (галочка)
        Button btnConfirm = new Button("✓ Подтвердить");
        btnConfirm.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnConfirm.setPrefWidth(130);
        btnConfirm.setOnAction(event -> confirmQuestionnaire(id, cardBox));

        // Кнопка "Отклонить" (крестик)
        Button btnReject = new Button("✗ Отклонить");
        btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnReject.setPrefWidth(130);
        btnReject.setOnAction(event -> rejectQuestionnaire(id, cardBox));

        actionBox.getChildren().addAll(btnView, btnConfirm, btnReject);

        cardBox.getChildren().addAll(headerBox, infoGrid, actionBox);

        questionnairesContainer.getChildren().add(cardBox);
    }

    /**
     * Добавление строки информации в GridPane
     */
    private void addInfoRow(GridPane grid, int rowIndex, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        Label value = new Label(valueText != null ? valueText : "Нет данных");
        value.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; " +
                "-fx-background-color: #f9f9f9; -fx-padding: 5 10 5 10; " +
                "-fx-background-radius: 3; -fx-border-radius: 3; " +
                "-fx-border-color: #bdc3c7; -fx-border-width: 1;");
        value.setMinWidth(200);

        grid.add(label, 0, rowIndex);
        grid.add(value, 1, rowIndex);
    }

    /**
     * Создание ColumnConstraints
     */
    private ColumnConstraints createColumnConstraint(double prefWidth) {
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPrefWidth(prefWidth);
        cc.setHalignment(javafx.geometry.HPos.LEFT);
        return cc;
    }

    /**
     * Просмотр анкеты
     */
    private void viewQuestionnaire(int id) {
        MainMenuControl mainMenuControl = MainMenuControl.getInstance();
        QuestionnaireViewUpdateWindow.id = id;
        mainMenuControl.showViewForTab("Анкета обновления");
    }

    /**
     * Подтверждение анкеты
     */
    private void confirmQuestionnaire(int id, VBox cardBox) {
        TempUpdateData data = tempUpdateData.get(id);
        if (data == null) {
            showNotification("Данные анкеты не найдены", false);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение анкеты");
        alert.setHeaderText("Подтвердить анкету #" + (id + 1) + "?");
        alert.setContentText("Анкета будет помечена как проверенная и сохранена в базе данных.");

        ButtonType confirmBtn = new ButtonType("Подтвердить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == confirmBtn) {
            QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();

            Questionnaire questionnaire = data.getQuestionnaire();
            Patient patient = data.getPatient();
            List<CharacterizationAnketPatient> characteristics = data.getCharacteristics();

            boolean success = questionnaireManagerModule.saveQuestionnaire(questionnaire, patient, characteristics);

            if (success) {
                showNotification("Анкета #" + (id + 1) + " подтверждена", true);
                cardBox.setStyle("-fx-border-color: #27ae60; -fx-border-radius: 5; " +
                        "-fx-background-radius: 5; -fx-padding: 15;" +
                        "-fx-background-color: #e8f8f5; -fx-effect: dropshadow(gaussian, rgba(39,174,96,0.3), 10, 0, 0, 2);");

                // Удаляем из временного хранилища
                tempUpdateData.remove(id);

                // Удаляем карточку из UI через небольшую задержку
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        Platform.runLater(() -> {
                            questionnairesContainer.getChildren().remove(cardBox);
                            updateCount();
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showNotification("Ошибка при подтверждении анкеты", false);
            }
        }
    }

    /**
     * Отклонение анкеты
     */
    private void rejectQuestionnaire(int id, VBox cardBox) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Отказ анкеты");
        alert.setHeaderText("Отказать анкете #" + (id + 1) + "?");
        alert.setContentText("Анкета будет удалена и не сохранена в базе данных.");

        ButtonType confirmBtn = new ButtonType("Подтвердить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == confirmBtn) {
            showNotification("Анкета #" + (id + 1) + " отклонена", true);
            cardBox.setStyle("-fx-border-color: #e74c3c; -fx-border-radius: 5; " +
                    "-fx-background-radius: 5; -fx-padding: 15;" +
                    "-fx-background-color: #fdedec; -fx-effect: dropshadow(gaussian, rgba(231,76,60,0.3), 10, 0, 0, 2);");

            // Удаляем из временного хранилища
            tempUpdateData.remove(id);

            // Удаляем карточку из UI через небольшую задержку
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(() -> {
                        questionnairesContainer.getChildren().remove(cardBox);
                        updateCount();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Подтверждение всех анкет
     */
    private void confirmAllQuestionnaires() {
        if (tempUpdateData.isEmpty()) {
            showNotification("Нет анкет для подтверждения", false);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение всех анкет");
        alert.setHeaderText("Подтвердить все анкеты (" + tempUpdateData.size() + " шт.)?");
        alert.setContentText("Это действие нельзя отменить.");

        ButtonType confirmBtn = new ButtonType("Подтвердить все", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == confirmBtn) {
            QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
            int successCount = 0;

            for (Map.Entry<Integer, TempUpdateData> entry : new HashMap<>(tempUpdateData).entrySet()) {
                TempUpdateData data = entry.getValue();
                boolean success = questionnaireManagerModule.saveQuestionnaire(
                        data.getQuestionnaire(),
                        data.getPatient(),
                        data.getCharacteristics()
                );

                if (success) {
                    successCount++;
                    tempUpdateData.remove(entry.getKey());
                }
            }

            showNotification("Подтверждено анкет: " + successCount + " из " + tempUpdateData.size(), true);

            // Обновляем UI - удаляем все карточки
            questionnairesContainer.getChildren().clear();
            if (tempUpdateData.isEmpty()) {
                showEmptyMessage();
            } else {
                // Пересоздаем оставшиеся карточки
                for (Map.Entry<Integer, TempUpdateData> entry : tempUpdateData.entrySet()) {
                    createQuestionnaireCard(
                            entry.getKey(),
                            entry.getValue().getQuestionnaire(),
                            entry.getValue().getPatient(),
                            entry.getValue().getCharacteristics()
                    );
                }
            }
            updateCount();
        }
    }

    /**
     * Обновление счетчика
     */
    private void updateCount() {
        lblCount.setText("Найдено: " + tempUpdateData.size());
    }

    /**
     * Показ уведомления
     */
    private void showNotification(String message, boolean isSuccess) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(isSuccess ? "Успешно" : "Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);

        if (!isSuccess) {
            alert.getDialogPane().setStyle("-fx-background-color: #fdedec;");
        }

        alert.showAndWait();
    }

    /**
     * Внутренний класс для хранения временных данных обновления
     */
    private static class TempUpdateData {
        private final Patient patient;
        private final Questionnaire questionnaire;
        private final List<CharacterizationAnketPatient> characteristics;

        public TempUpdateData(Patient patient, Questionnaire questionnaire,
                              List<CharacterizationAnketPatient> characteristics) {
            this.patient = patient;
            this.questionnaire = questionnaire;
            this.characteristics = characteristics;
        }

        public Patient getPatient() { return patient; }
        public Questionnaire getQuestionnaire() { return questionnaire; }
        public List<CharacterizationAnketPatient> getCharacteristics() { return characteristics; }
    }
}