package com.pancreatitis.ui; // или ваш пакет

import com.pancreatitis.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PanelController {
    @FXML private Label userNameLabel; // предполагается, что в panelView.fxml есть Label с id="userNameLabel"

    @FXML
    private void initialize() {
        User user = User.getInstance();
        if (user != null && user.getLogin() != null && !user.getLogin().isEmpty()) {
            userNameLabel.setText(user.getLogin());
        }
    }
}