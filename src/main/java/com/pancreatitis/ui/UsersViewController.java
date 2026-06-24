package com.pancreatitis.ui;

import com.pancreatitis.models.Doctor;
import com.pancreatitis.modules.bip39.Bip39Encoder;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UsersViewController implements Initializable {

    @FXML private TableView<UserUI> usersTable;
    @FXML private TableColumn<UserUI, String> colLogin;
    @FXML private TableColumn<UserUI, String> colFullName;
    @FXML private TableColumn<UserUI, String> colEmail;
    @FXML private TableColumn<UserUI, String> colPhoneNumber;
    @FXML private TableColumn<UserUI, Boolean> colRole;
    @FXML private TableColumn<UserUI, Void> colPhrase;
    @FXML private TableColumn<UserUI, Void> colDelete; // новая колонка
    @FXML private TextField searchField;
    @FXML private Label statusLabel;

    private final ObservableList<UserUI> usersData = FXCollections.observableArrayList();
    private final FilteredList<UserUI> filteredData = new FilteredList<>(usersData, p -> true);
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Map<String, OpeningTask> openingTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(4);
    private final ExecutorService deleteExecutor = Executors.newSingleThreadExecutor(); // для удаления

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

        // Настройка столбца с кнопкой "Открыть" и обратным отсчётом
        colPhrase.setCellFactory(column -> new TableCell<>() {
            private final Button openButton = new Button("Открыть");
            private Label statusLabel = new Label();

            {
                openButton.setOnAction(event -> {
                    UserUI user = getTableView().getItems().get(getIndex());
                    if (user != null) {
                        startOpening(user.getLogin());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                UserUI user = getTableView().getItems().get(getIndex());
                if (user == null) {
                    setGraphic(null);
                    return;
                }
                String login = user.getLogin();
                OpeningTask task = openingTasks.get(login);

                if (task != null) {
                    statusLabel = new Label();
                    statusLabel.textProperty().bind(task.statusProperty());
                    setGraphic(statusLabel);
                } else {
                    setGraphic(openButton);
                }
            }
        });

        // Настройка столбца "Удалить"
        colDelete.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("Удалить");

            {
                deleteButton.setOnAction(event -> {
                    UserUI user = getTableView().getItems().get(getIndex());
                    if (user != null) {
                        // Показываем диалог подтверждения с обратным отсчётом
                        boolean confirmed = confirmDeleteWithCountdown(
                                "Удалить пользователя " + user.getLogin() + "?",
                                10 // секунд
                        );
                        if (confirmed) {
                            deleteUser(user);
                        }
                    }
                });
                // Стиль кнопки (красный)
                deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                }
            }
        });

        // Wrap FilteredList in SortedList and bind to comparator
        SortedList<UserUI> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(usersTable.comparatorProperty());

        // Bind data to table
        usersTable.setItems(sortedData);

        // Load data
        loadUsersData();

        // Регистрируем shutdown для executor'ов
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            deleteExecutor.shutdownNow();
            taskScheduler.shutdownNow();
        }));
    }

    /**
     * Универсальный диалог подтверждения с обратным отсчётом.
     * Блокирует весь интерфейс (модальный).
     *
     * @param message  сообщение
     * @param seconds  количество секунд до автоматического подтверждения
     * @return true, если подтверждено (таймер дошёл до нуля), false при отмене
     */
    private boolean confirmDeleteWithCountdown(String message, int seconds) {
        AtomicBoolean confirmed = new AtomicBoolean(false);
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Подтверждение удаления");
        dialogStage.setResizable(false);

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20; -fx-background-color: #f0f0f0;");

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label countdownLabel = new Label("Осталось: " + seconds + " с");
        countdownLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #d9534f;");

        Button cancelButton = new Button("Отменить");
        cancelButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 14px;");
        cancelButton.setPrefWidth(120);

        root.getChildren().addAll(messageLabel, countdownLabel, cancelButton);

        Scene scene = new Scene(root, 400, 200);
        dialogStage.setScene(scene);

        // Таймер
        AtomicInteger remaining = new AtomicInteger(seconds);
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(1), event -> {
                    int left = remaining.decrementAndGet();
                    if (left <= 0) {
                        // Время вышло — закрываем диалог и подтверждаем
                        dialogStage.close();
                        confirmed.set(true);
                    } else {
                        countdownLabel.setText("Осталось: " + left + " с");
                    }
                })
        );
        timeline.setCycleCount(seconds); // количество кадров
        timeline.play();

        // Обработчик отмены
        cancelButton.setOnAction(e -> {
            timeline.stop();
            dialogStage.close();
            confirmed.set(false);
        });

        // Если пользователь закрыл окно крестиком — считаем отменой
        dialogStage.setOnCloseRequest(e -> {
            timeline.stop();
            confirmed.set(false);
        });

        dialogStage.showAndWait();
        return confirmed.get();
    }

    /**
     * Запускает процесс для указанного логина.
     */
    private void startOpening(String login) {
        OpeningTask existing = openingTasks.remove(login);
        if (existing != null) {
            existing.stopAndRemove();
        }

        OpeningTask task = new OpeningTask(login);
        openingTasks.put(login, task);
        task.start();

        Platform.runLater(() -> usersTable.refresh());
    }

    /**
     * Загружает пользователей из БД и очищает завершённые задачи.
     */
    private void loadUsersData() {
        try {
            LocalStorageModule localStorageModule = LocalStorageModule.getInstance();
            DatabaseModule databaseModule = DatabaseModule.getInstance();
            List<String> allLogins = localStorageModule.getAllUserLogins();
            List<UserUI> users = new ArrayList<>();

            for (String login : allLogins) {
                Doctor doctor = databaseModule.getDoctorByLogin(login);
                if (doctor != null) users.add(new UserUI(doctor));
            }

            usersData.setAll(users);
            showStatus("Загружено пользователей: " + usersData.size(), Color.GREEN);

            // Очищаем завершённые задачи (чтобы кнопки "Открыть" снова стали доступны)
            Iterator<Map.Entry<String, OpeningTask>> it = openingTasks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, OpeningTask> entry = it.next();
                if (entry.getValue().isDone()) {
                    it.remove();
                }
            }

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

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Результат операции");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Удаление пользователя (выполняется в фоновом потоке).
     */
    private void deleteUser(UserUI user) {
        String login = user.getLogin();
        deleteExecutor.submit(() -> {
            try {
                DatabaseModule db = DatabaseModule.getInstance();
                Doctor doctor = db.getDoctorByLogin(login);
                if (doctor == null) {
                    Platform.runLater(() -> showStatus("Пользователь не найден", Color.RED));
                    return;
                }
                int doctorId = doctor.getId();

                // Удаление из БД (анкеты, обнуление эксперта, сам доктор)
                db.deleteDoctor(doctorId);

                // Удаление локальной папки пользователя
                LocalStorageModule.getInstance().deleteUserData(login);

                Platform.runLater(() -> {
                    loadUsersData(); // обновить таблицу
                    showStatus("Пользователь " + login + " успешно удалён", Color.GREEN);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showStatus("Ошибка при удалении: " + e.getMessage(), Color.RED);
                });
                e.printStackTrace();
            }
        });
    }

    // ==================== ВНУТРЕННИЙ КЛАСС ДЛЯ УПРАВЛЕНИЯ ЗАДАЧЕЙ ====================

    private class OpeningTask {
        private final String login;
        private final StringProperty status = new SimpleStringProperty("Открыть");
        private final AtomicInteger secondsLeft = new AtomicInteger(60); // 60 секунд
        private volatile boolean done = false;
        private volatile boolean finished = false;
        private String finalStatus = "";

        private ScheduledFuture<?> countdownFuture;
        private ScheduledFuture<?> checkFuture;

        public OpeningTask(String login) {
            this.login = login;
        }

        public void start() {
            try {
                byte[] key = LocalStorageModule.getInstance().downloadUserKey(login);
                String mnemonic = Bip39Encoder.toMnemonic(key);
                boolean uploaded = CloudStorageModule.getInstance().uploadUserWords(login, mnemonic);
                if (!uploaded) {
                    finishWithStatus("Ошибка загрузки");
                    return;
                }

                countdownFuture = taskScheduler.scheduleAtFixedRate(() -> {
                    if (finished) return;
                    int left = secondsLeft.decrementAndGet();
                    if (left <= 0) {
                        boolean exists = CloudStorageModule.getInstance().checkUserWordsExists(login);
                        if (exists) {
                            CloudStorageModule.getInstance().deleteUserWords(login);
                            finishWithStatus("Время истекло");
                        } else {
                            finishWithStatus("Успешно");
                        }
                    } else {
                        updateStatus("Осталось: " + left + "с");
                    }
                }, 1, 1, TimeUnit.SECONDS);

                checkFuture = taskScheduler.scheduleAtFixedRate(() -> {
                    if (finished || done) return;
                    boolean exists = CloudStorageModule.getInstance().checkUserWordsExists(login);
                    if (!exists) {
                        finishWithStatus("Успешно");
                    }
                }, 3, 3, TimeUnit.SECONDS);

            } catch (Exception e) {
                finishWithStatus("Ошибка: " + e.getMessage());
            }
        }

        private void finishWithStatus(String text) {
            synchronized (this) {
                if (finished) return;
                finished = true;
            }
            done = true;
            finalStatus = text;
            stopTimers();

            // Удаляем задачу из карты, чтобы в ячейке снова появилась кнопка
            openingTasks.remove(login);

            // Показываем Alert
            Platform.runLater(() -> showAlert(text));

            // Обновляем таблицу, чтобы ячейка перерисовалась (теперь задачи нет)
            Platform.runLater(() -> usersTable.refresh());
        }

        private void updateStatus(String text) {
            Platform.runLater(() -> status.set(text));
        }

        private void stopTimers() {
            if (countdownFuture != null && !countdownFuture.isDone()) {
                countdownFuture.cancel(false);
            }
            if (checkFuture != null && !checkFuture.isDone()) {
                checkFuture.cancel(false);
            }
        }

        public void stopAndRemove() {
            stopTimers();
            done = true;
            openingTasks.remove(login);
        }

        public StringProperty statusProperty() {
            return status;
        }

        public boolean isDone() {
            return done;
        }
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

        public UserUI(Doctor user) {
            this.login = new SimpleStringProperty(user.getLogin());
            this.fullName = new SimpleStringProperty(user.getFio());
            this.email = new SimpleStringProperty(user.getEmail());
            this.phone = new SimpleStringProperty(user.getPhone());
            this.expert = new SimpleBooleanProperty(user.getStatus());
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