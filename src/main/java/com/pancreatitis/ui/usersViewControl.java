package com.pancreatitis.ui;

import com.pancreatitis.models.User;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class usersViewControl implements Initializable {

    @FXML private TableView<UserUI> usersTable;
    @FXML private TableColumn<UserUI, String> colNamePerson;
    @FXML private TableColumn<UserUI, LocalDate> colDataReg;
    @FXML private TableColumn<UserUI, String> colEmail;
    @FXML private TableColumn<UserUI, String> colPhoneNumber;
    @FXML private TableColumn<UserUI, Boolean> colRole;

    private final ObservableList<UserUI> usersData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup columns with value factories
        colNamePerson.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhoneNumber.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colDataReg.setCellValueFactory(new PropertyValueFactory<>("registrationDate"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("expert"));

        // Format date column
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        colDataReg.setCellFactory(column -> new TextFieldTableCell<UserUI, LocalDate>() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(formatter));
            }
        });

        // Format role column (Эксперт/Пользователь)
        colRole.setCellFactory(column -> new TextFieldTableCell<UserUI, Boolean>() {
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
        colNamePerson.setCellFactory(TextFieldTableCell.forTableColumn());
        colEmail.setCellFactory(TextFieldTableCell.forTableColumn());
        colPhoneNumber.setCellFactory(TextFieldTableCell.forTableColumn());

        // Bind data to table
        usersTable.setItems(usersData);

        // Load placeholder data
        //loadPlaceholderData2();
    }

    /**
     * Placeholder data - replace with real data loading logic later
     */
    private void loadPlaceholderData() {
        usersData.addAll(
                new UserUI("Иванов Иван Иванович", LocalDate.of(2025, 1, 15), "ivanov@example.com", "+7 (900) 123-45-67", true),
                new UserUI("Петрова Анна Сергеевна", LocalDate.of(2025, 2, 20), "petrova@example.com", "+7 (900) 234-56-78", false),
                new UserUI("Сидоров Дмитрий Владимирович", LocalDate.of(2025, 3, 5), "sidorov@example.com", "+7 (900) 345-67-89", true),
                new UserUI("Козлова Екатерина Андреевна", LocalDate.of(2025, 4, 12), "kozlova@example.com", "+7 (900) 456-78-90", false),
                new UserUI("Морозов Алексей Петрович", LocalDate.of(2025, 5, 30), "morozov@example.com", "+7 (900) 567-89-01", true)

        );
    }
    /*private void loadPlaceholderData2() {
        CloudStorageModule cloudStorageModule = CloudStorageModule.getInstance();
        List<User> allUsers = cloudStorageModule.getAllUsers();
        ArrayList<UserUI> users = new ArrayList<>();
        for (User user: allUsers){
            users.add(new UserUI(user));
        }
        usersData.addAll(users);
    }*/

    /**
     * User model class - replace with your actual domain model later
     */
    public static class UserUI {
        private final StringProperty name;
        private final ObjectProperty<LocalDate> registrationDate;
        private final StringProperty email;
        private final StringProperty phone;
        private final BooleanProperty expert;

        public UserUI(String name, LocalDate registrationDate, String email, String phone, boolean expert) {
            this.name = new SimpleStringProperty(name);
            this.registrationDate = new SimpleObjectProperty<>(registrationDate);
            this.email = new SimpleStringProperty(email);
            this.phone = new SimpleStringProperty(phone);
            this.expert = new SimpleBooleanProperty(expert);
        }

        public UserUI(User user) {
            this.name = new SimpleStringProperty(user.getFullName());
            this.registrationDate = new SimpleObjectProperty<>(LocalDate.of(2025, 3, 5));
            this.email = new SimpleStringProperty(user.getEmail() != null ? user.getEmail() : "");
            this.phone = new SimpleStringProperty(user.getPhone() != null ? user.getPhone() : "");
            this.expert = new SimpleBooleanProperty(
                    user.getDoctor() != null && user.getDoctor().getStatus() != null ? user.getDoctor().getStatus() : false
            );
        }

        // Getters for JavaFX binding
        public String getName() { return name.get(); }
        public StringProperty nameProperty() { return name; }
        public void setName(String name) { this.name.set(name); }

        public LocalDate getRegistrationDate() { return registrationDate.get(); }
        public ObjectProperty<LocalDate> registrationDateProperty() { return registrationDate; }
        public void setRegistrationDate(LocalDate registrationDate) { this.registrationDate.set(registrationDate); }

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

    // Public method for external data loading (to be used by service layer later)
    public void setUsersData(ObservableList<UserUI> userUIS) {
        usersData.setAll(userUIS);
    }

    public ObservableList<UserUI> getUsersData() {
        return usersData;
    }
}