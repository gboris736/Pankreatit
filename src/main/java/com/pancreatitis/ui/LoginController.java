package com.pancreatitis.ui;

import com.pancreatitis.modules.authorization.AuthorizationModule;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.crypto.SecretKey;

public class LoginController {
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private String login;
    private SecretKey secretKey;
    private boolean authenticated = false;

    @FXML
    private void handleLogin() {
        String login = loginField.getText().trim();
        String password = passwordField.getText();

        if (login.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Введите логин и пароль");
            return;
        }

        try {
            SecretKey key = AuthorizationModule.getInstance().authenticateUser(login, password);
            if (key != null) {
                this.login = login;
                this.secretKey = key;
                this.authenticated = true;
                ((Stage) loginField.getScene().getWindow()).close();
            } else {
                errorLabel.setText("Неверный логин или пароль");
            }
        } catch (Exception e) {
            // Любое исключение теперь тоже выдаёт "Неверный логин или пароль"
            errorLabel.setText("Неверный логин или пароль");
        }
    }

    public boolean isAuthenticated() { return authenticated; }
    public String getLogin() { return login; }
    public SecretKey getSecretKey() { return secretKey; }
}