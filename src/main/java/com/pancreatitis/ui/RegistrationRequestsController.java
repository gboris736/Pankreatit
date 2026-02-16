package com.pancreatitis.ui;

import com.pancreatitis.models.RegistrationForm;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/***
Добавить обработку регистрации и метод получения заявок

 */


public class RegistrationRequestsController {

    @FXML private TableView<RegistrationForm> tableView;
    @FXML private TableColumn<RegistrationForm, String> colLogin;
    @FXML private TableColumn<RegistrationForm, String> colFullName;
    @FXML private TableColumn<RegistrationForm, String> colEmail;
    @FXML private TableColumn<RegistrationForm, String> colNumber;
    @FXML private TableColumn<RegistrationForm, String> colStatus;
    @FXML private TableColumn<RegistrationForm, Void> colActions;       //Колонка для кнопок
    @FXML private Label statusLabel;

    private final ObservableList<RegistrationForm> registrationList = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // Для реального приложения: внедрить через конструктор или сервис-локатор
    // private final RegistrationService registrationService = new RegistrationService();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadRegistrationRequests();
        setupStatusUpdates();
    }

    private void setupTableColumns() {
        colLogin.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLogin()));
        colFullName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));
        colEmail.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmail()));
        colNumber.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNumber()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty("Ожидает"));

        // Динамические кнопки в строке таблицы
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button approveBtn = createButton("✅", "approve");
            private final Button rejectBtn = createButton("❌", "reject");
            private final HBox container = new HBox(5, approveBtn, rejectBtn);

            {
                container.setAlignment(javafx.geometry.Pos.CENTER);
                container.setPadding(new Insets(2));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    RegistrationForm form = getTableRow().getItem();
                    approveBtn.setOnAction(e -> handleApprove(form));
                    rejectBtn.setOnAction(e -> handleReject(form));
                    setGraphic(container);
                }
            }
        });
    }

    private Button createButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.setMinWidth(30);
        btn.getStyleClass().add("table-button");
        btn.getStyleClass().add("table-button-" + styleClass);
        btn.setOnAction(e -> e.consume()); // Предотвращает снятие выделения строки
        return btn;
    }

    private void setupStatusUpdates() {
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                statusLabel.setText("Выбрана заявка: " + newVal.getLogin() + " (" + newVal.getFullName() + ")");
            } else {
                statusLabel.setText("");
            }
        });
    }

    // ЗАГЛУШКА: заменить на вызов сервиса в реальном приложении
    private void loadRegistrationRequests() {
        registrationList.clear();

        // Пример тестовых данных
        registrationList.addAll(List.of(
                new RegistrationForm("ivanov", "******", "Иванов Иван Иванович", "ivanov@mail.ru", "+7 (999) 123-45-67"),
                new RegistrationForm("petrova", "******", "Петрова Мария Сергеевна", "petrova@example.com", "+7 (987) 654-32-10"),
                new RegistrationForm("smirnov", "******", "Смирнов Алексей Дмитриевич", "smirnov@domain.com", "+7 (912) 345-67-89")
        ));

        // Реальная реализация:
//         try {
//             registrationList.setAll(registrationService.getPendingRegistrations());
//         } catch (Exception e) {
//             showError("Ошибка загрузки заявок: " + e.getMessage());
//         }

        tableView.setItems(registrationList);
        showStatus("Загружено заявок: " + registrationList.size());
    }

    @FXML
    private void onRefresh() {
        loadRegistrationRequests();
        showStatus("Данные обновлены: " + dtf.format(LocalDateTime.now()));
    }

    private void handleApprove(RegistrationForm form) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Заявка одобрена");
        alert.setHeaderText("Пользователь '" + form.getLogin() + "' успешно зарегистрирован");
        alert.setContentText("Логин: " + form.getLogin() + "\nФИО: " + form.getFullName());
        alert.showAndWait();

        // registrationService.approveRequest(form.getLogin());
        registrationList.remove(form);
        showStatus("Заявка одобрена и удалена из списка: " + form.getLogin());
    }

    private void handleReject(RegistrationForm form) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Отклонение заявки");
        confirm.setHeaderText("Отклонить заявку пользователя " + form.getLogin() + "?");
        confirm.setContentText("Укажите причину (опционально):");

        // Добавляем поле ввода причины
        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Причина отклонения...");
        reasonArea.setPrefRowCount(3);
        confirm.getDialogPane().setContent(reasonArea);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String reason = reasonArea.getText().trim();
            // registrationService.rejectRequest(form.getLogin(), reason);

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Заявка отклонена");
            info.setHeaderText("Заявка пользователя '" + form.getLogin() + "' отклонена");
            if (!reason.isEmpty()) info.setContentText("Причина: " + reason);
            info.showAndWait();

            registrationList.remove(form);
            showStatus("Заявка отклонена: " + form.getLogin());
        }
    }


    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
        statusLabel.setOpacity(1.0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                javafx.util.Duration.seconds(5), statusLabel);
        ft.setToValue(0.3);
        ft.play();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);

        alert.initOwner(primaryStage);
        alert.setTitle("Внимание");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        statusLabel.setText("⚠ " + message);
        statusLabel.setTextFill(javafx.scene.paint.Color.ORANGE);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        statusLabel.setText("❌ " + message);
        statusLabel.setTextFill(javafx.scene.paint.Color.RED);
    }
}