package com.pancreatitis.ui;

import com.pancreatitis.models.User;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class UsersViewController implements Initializable {

    @FXML private TableView<UserUI> usersTable;
    @FXML private TableColumn<UserUI, String> colLogin;
    @FXML private TableColumn<UserUI, String> colFullName;
    @FXML private TableColumn<UserUI, String> colEmail;
    @FXML private TableColumn<UserUI, String> colPhoneNumber;
    @FXML private TableColumn<UserUI, Boolean> colRole;
    @FXML private TextField searchField;
    @FXML private Label statusLabel;

    private final ObservableList<UserUI> usersData = FXCollections.observableArrayList();
    private final FilteredList<UserUI> filteredData = new FilteredList<>(usersData, p -> true);
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup columns with value factories
        colLogin.setCellValueFactory(new PropertyValueFactory<>("login"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhoneNumber.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("expert"));

        // Format role column (Эксперт/Пользователь)
        colRole.setCellFactory(column -> new TableCell<UserUI, Boolean>() {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item ? "Эксперт" : "Пользователь");
                    setStyle(item ?
                            "-fx-text-fill: #28a745; -fx-font-weight: bold;" :
                            "-fx-text-fill: #6c757d;");
                }
            }
        });

        // Make columns editable (optional)
        colLogin.setCellFactory(TextFieldTableCell.forTableColumn());
        colFullName.setCellFactory(TextFieldTableCell.forTableColumn());
        colEmail.setCellFactory(TextFieldTableCell.forTableColumn());
        colPhoneNumber.setCellFactory(TextFieldTableCell.forTableColumn());

        // Setup search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return user.getLogin().toLowerCase().contains(lowerCaseFilter) ||
                        user.getFullName().toLowerCase().contains(lowerCaseFilter) ||
                        user.getEmail().toLowerCase().contains(lowerCaseFilter) ||
                        user.getPhone().toLowerCase().contains(lowerCaseFilter);
            });
        });

        // Wrap FilteredList in SortedList and bind to comparator
        SortedList<UserUI> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(usersTable.comparatorProperty());

        // Bind data to table
        usersTable.setItems(sortedData);

        // Load data
        loadUsersData();
    }

    /**
     * Load users from cloud storage
     */
    private void loadUsersData() {
        try {
            CloudStorageModule cloudStorageModule = CloudStorageModule.getInstance();
            List<User> allUsers = cloudStorageModule.getAllUsers();
            List<UserUI> users = new ArrayList<>();

            for (User user : allUsers) {
                users.add(new UserUI(user));
            }

            usersData.setAll(users);
            showStatus("Загружено пользователей: " + usersData.size(), Color.GREEN);

        } catch (Exception e) {
            showStatus("Ошибка загрузки данных: " + e.getMessage(), Color.RED);
            e.printStackTrace();
        }
    }

    @FXML
    private void onRefresh() {
        loadUsersData();
        showStatus("Данные обновлены: " + dtf.format(LocalDateTime.now()), Color.GREEN);
    }

    @FXML
    private void onSearch() {
        // Search is handled automatically by the listener
        showStatus("Найдено записей: " + filteredData.size(), Color.BLUE);
    }

    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
        statusLabel.setOpacity(1.0);

        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                javafx.util.Duration.seconds(5), statusLabel);
        ft.setToValue(0.3);
        ft.play();
    }

    /**
     * User model class for UI representation
     */
    public static class UserUI {
        private final StringProperty login;
        private final StringProperty fullName;
        private final StringProperty email;
        private final StringProperty phone;
        private final BooleanProperty expert;

        public UserUI(String login, String fullName, String email, String phone, boolean expert) {
            this.login = new SimpleStringProperty(login);
            this.fullName = new SimpleStringProperty(fullName);
            this.email = new SimpleStringProperty(email);
            this.phone = new SimpleStringProperty(phone);
            this.expert = new SimpleBooleanProperty(expert);
        }

        public UserUI(User user) {
            this.login = new SimpleStringProperty(user.getLogin());
            this.fullName = new SimpleStringProperty(user.getFullName());
            this.email = new SimpleStringProperty(user.getEmail());
            this.phone = new SimpleStringProperty(user.getPhone());
            this.expert = new SimpleBooleanProperty(
                    user.getDoctor() != null ? user.getDoctor().getStatus() : false
            );
        }

        // Getters and property getters for JavaFX binding
        public String getLogin() { return login.get(); }
        public StringProperty loginProperty() { return login; }
        public void setLogin(String login) { this.login.set(login); }

        public String getFullName() { return fullName.get(); }
        public StringProperty fullNameProperty() { return fullName; }
        public void setFullName(String fullName) { this.fullName.set(fullName); }

        public String getEmail() { return email.get(); }
        public StringProperty emailProperty() { return email; }
        public void setEmail(String email) { this.email.set(email); }

        public String getPhone() { return phone.get(); }
        public StringProperty phoneProperty() { return phone; }
        public void setPhone(String phone) { this.phone.set(phone); }

        public boolean isExpert() { return expert.get(); }
        public BooleanProperty expertProperty() { return expert; }
        public void setExpert(boolean expert) { this.expert.set(expert); }
    }
}