package com.pancreatitis.ui;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.questionnairemanager.QuestionnaireManagerModule;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    private final Map<Integer, QuestionnaireCard> questionnaireCards = new HashMap<>();
    private final List<Questionnaire> pendingQuestionnaires = new ArrayList<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        loadQuestionnaires();

        btnBack.setOnAction(event -> {
            MainMenuControl mainMenuControl = MainMenuControl.getInstance();
            mainMenuControl.showViewForTab("Главное меню");
        });

        btnRefresh.setOnAction(event -> loadQuestionnaires());

        btnConfirmAll.setOnAction(event -> confirmAllQuestionnaires());
    }

    /**
     * Загрузка анкет на верификацию
     */
    private void loadQuestionnaires() {
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        //QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
        questionnairesContainer.getChildren().clear();
        questionnaireCards.clear();
        pendingQuestionnaires.clear();

        // Получаем анкеты, требующие верификации
        //List<Questionnaire> questionnaires =
        List<Questionnaire> questionnaires = new ArrayList<>();

        pendingQuestionnaires.addAll(questionnaires);

        lblCount.setText("Найдено: " + questionnaires.size());

        for (Questionnaire questionnaire : questionnaires) {
            createQuestionnaireCard(questionnaire);
        }

        if (questionnaires.isEmpty()) {
            Label emptyLabel = new Label("Нет анкет на верификацию");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
            emptyLabel.setAlignment(Pos.CENTER);
            emptyLabel.setPrefWidth(Double.MAX_VALUE);
            questionnairesContainer.getChildren().add(emptyLabel);
        }
    }

    /**
     * Создание карточки анкеты
     */
    private void createQuestionnaireCard(Questionnaire questionnaire) {
        int id = (int)questionnaire.getId();

        // Основной блок карточки
        VBox cardBox = new VBox(10);
        cardBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; " +
                "-fx-background-radius: 5; -fx-padding: 15;" +
                "-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // Заголовок с ID и датой
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("Анкета #" + questionnaire.getId());
        idLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label dateLabel = new Label(questionnaire.getDateOfCompletion());
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(getStatusText(1));
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + getStatusColor(0) + "; " +
                "-fx-background-color: " + getStatusBackgroundColor(1) + "; " +
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
        addInfoRow(infoGrid, 0, "ФИО пациента:", getPatientFio((int)questionnaire.getIdPatient()));

        // Откуда поступил
        addInfoRow(infoGrid, 1, "Откуда поступил:", questionnaire.getAdmittedFrom());

        // Диагноз
        addInfoRow(infoGrid, 2, "Диагноз:", questionnaire.getDiagnosis());

        // Количество заполненных характеристик
        int characteristicsCount = getCharacteristicsCount((int)questionnaire.getId());
        addInfoRow(infoGrid, 3, "Заполнено характеристик:", String.valueOf(characteristicsCount));

        // Кнопки верификации
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));

        // Кнопка "Просмотр"
        Button btnView = new Button("📋 Просмотр");
        btnView.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnView.setPrefWidth(120);
        btnView.setOnAction(event -> viewQuestionnaire(questionnaire));

        // Кнопка "Подтвердить" (галочка)
        Button btnConfirm = new Button("✓ Подтвердить");
        btnConfirm.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnConfirm.setPrefWidth(130);
        btnConfirm.setOnAction(event -> confirmQuestionnaire(questionnaire, cardBox));

        // Кнопка "Отклонить" (крестик)
        Button btnReject = new Button("✗ Отклонить");
        btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnReject.setPrefWidth(130);
        btnReject.setOnAction(event -> rejectQuestionnaire(questionnaire, cardBox));

        actionBox.getChildren().addAll(btnView, btnConfirm, btnReject);

        cardBox.getChildren().addAll(headerBox, infoGrid, actionBox);

        questionnairesContainer.getChildren().add(cardBox);
        questionnaireCards.put(id, new QuestionnaireCard(cardBox, questionnaire));
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
        //cc.setHgrow(layout.Priority.SOMETIMES);
        cc.setHalignment(javafx.geometry.HPos.LEFT);
        return cc;
    }

    /**
     * Получение ФИО пациента
     */
    private String getPatientFio(int idPatient) {
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        Patient patient = databaseModule.getPatientById(idPatient);
        return patient != null ? patient.getFio() : "Неизвестно";
    }

    /**
     * Получение количества заполненных характеристик
     */
    private int getCharacteristicsCount(int idQuestionnaire) {
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        List<CharacterizationAnketPatient> characterizations =
                databaseModule.getCharacterizationsForAnket(idQuestionnaire);
        return characterizations != null ? characterizations.size() : 0;
    }

    /**
     * Просмотр анкеты
     */
    private void viewQuestionnaire(Questionnaire questionnaire) {
        MainMenuControl mainMenuControl = MainMenuControl.getInstance();
        MainMenuControl.idCurrentQuestionnaire = (int)questionnaire.getId();
        MainMenuControl.idCurrentPatient = (int)questionnaire.getIdPatient();
        mainMenuControl.showViewForTab("Анкета пациента");
    }

    /**
     * Подтверждение анкеты
     */
    private void confirmQuestionnaire(Questionnaire questionnaire, VBox cardBox) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение анкеты");
        alert.setHeaderText("Подтвердить анкету #" + questionnaire.getId() + "?");
        alert.setContentText("Анкета будет помечена как проверенная и сохранена в базе данных.");

        ButtonType confirmBtn = new ButtonType("Подтвердить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();

//        if (result.isPresent() && result.get() == confirmBtn) {
//            DatabaseModule databaseModule = DatabaseModule.getInstance();
//            boolean success = databaseModule.verifyQuestionnaire(questionnaire.getId(), true);
//
//            if (success) {
//                showNotification("Анкета #" + questionnaire.getId() + " подтверждена", true);
//                cardBox.setStyle("-fx-border-color: #27ae60; -fx-border-radius: 5; " +
//                        "-fx-background-radius: 5; -fx-padding: 15;" +
//                        "-fx-background-color: #e8f8f5; -fx-effect: dropshadow(gaussian, rgba(39,174,96,0.3), 10, 0, 0, 2);");
//
//                // Обновляем статус
//                questionnaire.setStatus(1); // 1 = подтверждено
//
//                // Удаляем карточку через паузу
//                PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(1));
//                pause.setOnFinished(event -> {
//                    questionnairesContainer.getChildren().remove(cardBox);
//                    questionnaireCards.remove(questionnaire.getId());
//                    pendingQuestionnaires.remove(questionnaire);
//                    lblCount.setText("Найдено: " + pendingQuestionnaires.size());
//                });
//                pause.play();
//            } else {
//                showNotification("Ошибка при подтверждении анкеты", false);
//            }
//        }
    }

    /**
     * Отклонение анкеты
     */
    private void rejectQuestionnaire(Questionnaire questionnaire, VBox cardBox) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Отклонение анкеты");
        dialog.setHeaderText("Отклонить анкету #" + questionnaire.getId() + "?");
        dialog.setContentText("Укажите причину отклонения (необязательно):");

        Optional<String> result = dialog.showAndWait();

//        if (result.isPresent()) {
//            String reason = result.get();
//            DatabaseModule databaseModule = DatabaseModule.getInstance();
//            boolean success = databaseModule.verifyQuestionnaire(questionnaire.getId(), false);
//
//            if (success) {
//                if (!reason.isEmpty()) {
//                    databaseModule.addRejectionReason(questionnaire.getId(), reason);
//                }
//
//                showNotification("Анкета #" + questionnaire.getId() + " отклонена", true);
//                cardBox.setStyle("-fx-border-color: #e74c3c; -fx-border-radius: 5; " +
//                        "-fx-background-radius: 5; -fx-padding: 15;" +
//                        "-fx-background-color: #fdedec; -fx-effect: dropshadow(gaussian, rgba(231,76,60,0.3), 10, 0, 0, 2);");
//
//                questionnaire.setStatus(2); // 2 = отклонено
//
//                PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(1));
//                pause.setOnFinished(event -> {
//                    questionnairesContainer.getChildren().remove(cardBox);
//                    questionnaireCards.remove(questionnaire.getId());
//                    pendingQuestionnaires.remove(questionnaire);
//                    lblCount.setText("Найдено: " + pendingQuestionnaires.size());
//                });
//                pause.play();
//            } else {
//                showNotification("Ошибка при отклонении анкеты", false);
//            }
//        }
    }

    /**
     * Подтверждение всех анкет
     */
    private void confirmAllQuestionnaires() {
        if (pendingQuestionnaires.isEmpty()) {
            showNotification("Нет анкет для подтверждения", false);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение всех анкет");
        alert.setHeaderText("Подтвердить все анкеты (" + pendingQuestionnaires.size() + " шт.)?");
        alert.setContentText("Это действие нельзя отменить.");

        ButtonType confirmBtn = new ButtonType("Подтвердить все", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();

//        if (result.isPresent() && result.get() == confirmBtn) {
//            DatabaseModule databaseModule = DatabaseModule.getInstance();
//            int successCount = 0;
//
//            for (Questionnaire questionnaire : new ArrayList<>(pendingQuestionnaires)) {
//                if (databaseModule.verifyQuestionnaire(questionnaire.getId(), true)) {
//                    successCount++;
//                }
//            }
//
//            showNotification("Подтверждено анкет: " + successCount + " из " + pendingQuestionnaires.size(), true);
//            loadQuestionnaires();
//        }
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
     * Текст статуса
     */
    private String getStatusText(int status) {
        return switch (status) {
            case 0 -> "На проверке";
            case 1 -> "Подтверждено";
            case 2 -> "Отклонено";
            default -> "Неизвестно";
        };
    }

    /**
     * Цвет текста статуса
     */
    private String getStatusColor(int status) {
        return switch (status) {
            case 0 -> "#f39c12";
            case 1 -> "#27ae60";
            case 2 -> "#e74c3c";
            default -> "#7f8c8d";
        };
    }

    /**
     * Цвет фона статуса
     */
    private String getStatusBackgroundColor(int status) {
        return switch (status) {
            case 0 -> "#fef5e7";
            case 1 -> "#e8f8f5";
            case 2 -> "#fdedec";
            default -> "#f5f5f5";
        };
    }

    /**
     * Внутренний класс для хранения информации о карточке
     */
    private static class QuestionnaireCard {
        private final VBox cardBox;
        private final Questionnaire questionnaire;

        public QuestionnaireCard(VBox cardBox, Questionnaire questionnaire) {
            this.cardBox = cardBox;
            this.questionnaire = questionnaire;
        }

        public VBox getCardBox() {
            return cardBox;
        }

        public Questionnaire getQuestionnaire() {
            return questionnaire;
        }
    }
}