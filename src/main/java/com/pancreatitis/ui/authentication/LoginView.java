package com.pancreatitis.ui.authentication;

import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.authorization.AuthorizationModule;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.ui.Launcher;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Objects;

public class LoginView {

    private VBox root;
    private TextField usernameField;
    private PasswordField passwordField;
    private Runnable onLoginSuccess;

    public LoginView() {
    }

    public Parent build(){
        root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("Вход в систему");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        Label usernameLabel = new Label("Логин:");
        usernameField = new TextField();
        usernameField.setPromptText("Введите логин");

        Label passwordLabel = new Label("Пароль:");
        passwordField = new PasswordField();
        passwordField.setPromptText("Введите пароль");

        Button loginButton = new Button("Войти");
        loginButton.setOnAction(e -> handleLogin());

        grid.add(usernameLabel, 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(passwordLabel, 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(loginButton, 1, 2);

        root.getChildren().addAll(titleLabel, grid);
        return root;
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        boolean successLogin = false;
        try {
            if (AuthorizationModule.getInstance().authenticateUser(username, password) != null) {
                successLogin = true;
            }
        } catch (Exception e) {
            successLogin = false;
        }

        if (successLogin) {
            if (onLoginSuccess != null){
                User user = null;
                for (User t : CloudStorageModule.getInstance().getAllUsers()){
                    if (Objects.equals(t.getLogin(), username)){
                        user = t;
                        break;
                    }
                }
                if (user != null) {
                    Launcher.getInstance().login(user);
                    onLoginSuccess.run();
                }
                else {
                    throw new RuntimeException("Хз что-то юзер не залогинился, оно не должно вообще так делать");
                }

            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка входа");
            alert.setHeaderText(null);
            alert.setContentText("Неверный логин или пароль.");
            alert.showAndWait();
        }


    }

    public void setOnLoginSuccess(Runnable action) {
        this.onLoginSuccess = action;
    }

    public VBox getRoot() {
        return root;
    }
}