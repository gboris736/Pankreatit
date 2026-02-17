package com.pancreatitis.ui;

import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
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
        loadPlaceholderData();
    }

    /**
     * Placeholder data - replace with real data loading logic later
     */
    private void loadPlaceholderData() {
        CloudStorageModule cloudStorageModule = CloudStorageModule.getInstance();
        List<User> allUsers = cloudStorageModule.getAllUsers();
        ArrayList<UserUI> users = new ArrayList<>();
        for(User user : allUsers){
            users.add(new UserUI(user));
        }
        usersData.addAll(
            users
        );
    }

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
            DatabaseModule databaseModule = DatabaseModule.getInstance();
            Doctor doctor = databaseModule.getDoctorByFio(user.getFullName());
            this.name = new SimpleStringProperty(doctor.getFio());
            this.registrationDate = new SimpleObjectProperty<>(LocalDate.of(2025, 1, 15));
            this.email = new SimpleStringProperty(user.getEmail());
            this.phone = new SimpleStringProperty(user.getPhone());
            this.expert = new SimpleBooleanProperty(doctor.getStatus());
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