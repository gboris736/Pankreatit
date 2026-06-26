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
import java.util.stream.Collectors;

public class QuestionRequestsList {

    @FXML
    private Button btnBack;
//    @FXML
//    private Button btnConfirmAll;
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

    private int totalUpdates = 0;
    private int loadedUpdates = 0;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        loadQuestionnaires();

        btnRefresh.setOnAction(event -> {
            if (!isLoading) {
                loadQuestionnaires();
            }
        });

        //btnConfirmAll.setOnAction(event -> confirmAllQuestionnaires());
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

        // Показываем индикатор загрузки
        showLoading(true, "Загрузка списка обновлений...");

        isLoading = true;
        totalUpdates = 0;
        loadedUpdates = 0;

        // Блокируем кнопки во время загрузки
        btnRefresh.setDisable(true);
        //btnConfirmAll.setDisable(true);

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
                        //btnConfirmAll.setDisable(false);
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
                        Questionnaire questionnaire = result.getQuestionnaire();
                        Patient patient = result.getPatient();
                        Doctor doctor = result.getDoctor();
                        List<CharacterizationAnketPatient> characteristics = result.getCharacteristics();

                        // Проверка существования анкеты в БД
                        boolean isExisting = false;
                        DatabaseModule db = DatabaseModule.getInstance();
                        int doctorId = (doctor != null) ? doctor.getId() : -1;
                        String completionDate = questionnaire.getDateOfCompletion();

                        if (completionDate != null) {
                            Questionnaire existingQ = db.findQuestionnaireByKey(doctorId, completionDate);
                            isExisting = (existingQ != null);
                        }

                        createQuestionnaireCard(
                                result.getIndex(),
                                questionnaire,
                                patient,
                                doctor,
                                isExisting
                        );
                    } else {
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

                    // Разблокируем кнопки
                    btnRefresh.setDisable(false);
                    //btnConfirmAll.setDisable(false);

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
                    //btnConfirmAll.setDisable(false);
                    showEmptyMessage();
                });
            }
        });
    }

    /**
     * Перестраивает UI из уже загруженных данных (без запросов к облаку).
     * Пересчитывает статус isExisting для каждой анкеты.
     */
    private void refreshFromExistingData() {
        // Очищаем текущий UI
        questionnairesContainer.getChildren().clear();

        List<Questionnaire> qList = updatesModule.getQuestionnairList();
        List<Patient> pList = updatesModule.getPatientList();
        List<Doctor> dList = updatesModule.getDoctors();
        List<List<CharacterizationAnketPatient>> cList = updatesModule.getCharacterizationAnketPatientList();

        totalUpdates = qList.size();
        lblCount.setText("Найдено: " + totalUpdates);

        if (totalUpdates == 0) {
            showEmptyMessage();
            return;
        }

        DatabaseModule db = DatabaseModule.getInstance();

        for (int i = 0; i < qList.size(); i++) {
            Questionnaire q = qList.get(i);
            Patient p = pList.get(i);
            Doctor d = dList.get(i);

            // Пересчитываем isExisting
            boolean isExisting = false;
            int doctorId = (d != null) ? d.getId() : -1;
            String completionDate = q.getDateOfCompletion();
            if (completionDate != null) {
                Questionnaire existingQ = db.findQuestionnaireByKey(doctorId, completionDate);
                isExisting = (existingQ != null);
            }

            createQuestionnaireCard(i, q, p, d, isExisting);
        }
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
    private void createQuestionnaireCard(int id, Questionnaire questionnaire, Patient patient, Doctor doctor, boolean isExisting) {
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

        String statusText = isExisting ? "Существующая анкета" : "Новая анкета";
        String statusColor = isExisting ? "#27ae60" : "#2980b9";
        String bgColor = isExisting ? "#e8f8f5" : "#eaf2f8";

        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + statusColor + "; " +
                "-fx-background-color: " + bgColor + "; " +
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

        // Врач
        addInfoRow(infoGrid, 3, "Ответственный врач:", doctor != null ? doctor.getFio() : "Не указан");

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
        QuestionnaireViewUpdate.id = id;
        mainMenuControl.showViewForTab("Анкета обновления");
    }

    /**
     * Подтверждение анкеты
     */
    private void confirmQuestionnaire(int id, VBox cardBox) {
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

            Questionnaire questionnaire = updatesModule.getQuestionnairList().get(id);
            Patient patient = updatesModule.getPatientList().get(id);
            Doctor doctor = updatesModule.getDoctors().get(id);
            List<CharacterizationAnketPatient> characteristics = updatesModule.getCharacterizationAnketPatientList().get(id);

            boolean isExisting = false;
            Questionnaire existingQ = null;
            DatabaseModule db = DatabaseModule.getInstance();
            int doctorId = (doctor != null) ? doctor.getId() : -1;
            String completionDate = questionnaire.getDateOfCompletion();

            if (completionDate != null) {
                existingQ = db.findQuestionnaireByKey(doctorId, completionDate);
                isExisting = (existingQ != null);
            }

            boolean success = false;
            if (isExisting) {
                List<CharacterizationAnketPatient> baseCharacterizationAnketPatientList = DatabaseModule.getInstance().getCharacterizationsForAnket((int)existingQ.getId());

                // Неправильное удаление - херь
                record Key(int idCharacteristic, int idValue, float value, String createdAt) {}

                Set<Key> keys = baseCharacterizationAnketPatientList.stream()
                        .map(p -> new Key(p.getIdCharacteristic(), p.getIdValue(), p.getValue(), p.getCreatedAt()))
                        .collect(Collectors.toSet());

                List<CharacterizationAnketPatient> resultChar = characteristics.stream()
                        .filter(p -> !keys.contains(new Key(p.getIdCharacteristic(), p.getIdValue(), p.getValue(), p.getCreatedAt())))
                        .collect(Collectors.toList());


                Patient our_patient = DatabaseModule.getInstance().getPatientById((int)existingQ.getIdPatient());

                success = questionnaireManagerModule.saveQuestionnaire(existingQ, our_patient, resultChar);
            } else {
                success = questionnaireManagerModule.saveQuestionnaire(questionnaire, patient, characteristics);
            }

            if (success) {
                showNotification("Анкета #" + (id + 1) + " подтверждена", true);
                cardBox.setStyle("-fx-border-color: #27ae60; -fx-border-radius: 5; " +
                        "-fx-background-radius: 5; -fx-padding: 15;" +
                        "-fx-background-color: #e8f8f5; -fx-effect: dropshadow(gaussian, rgba(39,174,96,0.3), 10, 0, 0, 2);");

                updatesModule.deleteUpdate(id);
                totalUpdates--;
                questionnairesContainer.getChildren().remove(cardBox);
                // Перестраиваем UI с актуальными данными и пересчитанными статусами
                refreshFromExistingData();
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

            updatesModule.deleteUpdate(id);
            totalUpdates--;
            questionnairesContainer.getChildren().remove(cardBox);
            // Перестраиваем UI с актуальными данными и пересчитанными статусами
            refreshFromExistingData();
        }
    }

    /**
     * Обновление счетчика
     */
    private void updateCount() {
        lblCount.setText("Найдено: " + totalUpdates);
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
}