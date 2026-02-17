package com.pancreatitis.ui;

import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.registration.RegistrationModule;
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

        // Реальная реализация:
         try {
             CloudStorageModule cloudStorageModule = CloudStorageModule.getInstance();
             registrationList.setAll(cloudStorageModule.getAllRegistrationForms());
         } catch (Exception e) {
             showError("Ошибка загрузки заявок: " + e.getMessage());
         }

        tableView.setItems(registrationList);
        showStatus("Загружено заявок: " + registrationList.size());
    }

    @FXML
    private void onRefresh() {
        loadRegistrationRequests();
        showStatus("Данные обновлены: " + dtf.format(LocalDateTime.now()));
    }

    private void handleApprove(RegistrationForm form) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтвердить одобрение");
        confirm.setHeaderText("Одобрить заявку пользователя " + form.getLogin() + "?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            RegistrationModule registrationModule = RegistrationModule.getInstance();
            registrationModule.acceptRegistrationRequest(form);

            registrationList.remove(form);
            showStatus("Заявка одобрена и удалена из списка: " + form.getLogin());
        } else {
            showStatus("Одобрение отменено: " + form.getLogin());
        }
    }

    private void handleReject(RegistrationForm form) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Отклонение заявки");
        confirm.setHeaderText("Отклонить заявку пользователя " + form.getLogin() + "?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            RegistrationModule registrationModule = RegistrationModule.getInstance();
            registrationModule.rejectRegistrationRequest(form);

            registrationList.remove(form);
            showStatus("Заявка отклонена: " + form.getLogin());
        } else {
            showStatus("Отклонение отменено: " + form.getLogin());
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