package com.pancreatitis.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.collections.FXCollections;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainMenuControl {
    public ImageView userAvatar;
    @FXML private Label userNameLabel;
    @FXML private ListView<String> tabsListView;
    @FXML private StackPane contentPane;
    //private final String absAdressFxmlFile = "resources.io.pankr.";

    // кеш загруженных view для быстрого переключения
    private final Map<String, Node> viewCache = new HashMap<>();

    @FXML
    public void initialize() {
        /*if (modelDataControl.getCurrentDoctor() == null) {
            try {
                throw new DoctorNotAuthenticatedException();
            }
            catch (DoctorNotAuthenticatedException e) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                Window owner = (tabsListView.getScene() != null) ? tabsListView.getScene().getWindow() : null;
                if (owner != null) {
                    alert.initOwner(owner);
                    alert.initModality(Modality.WINDOW_MODAL);
                } else {
                    alert.initModality(Modality.APPLICATION_MODAL);
                }
                ButtonType restartBtn = new ButtonType("Перезапустить (НЕ ТРОГАТЬ НЕ ЗАКОНЧЕНО");       // ДОДЕЛАТЬ
                ButtonType exitBtn = new ButtonType("Выйти");
                ButtonType cancelBtn = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(restartBtn, exitBtn, cancelBtn);


                alert.setOnShown(ev -> {
                    // Если есть владелец — центрируем относительно него, иначе центрируем на экране
                    Stage dialogStage = (Stage) alert.getDialogPane().getScene().getWindow();
                    if (owner != null) {
                        double centerX = owner.getX() + owner.getWidth() / 2.0;
                        double centerY = owner.getY() + owner.getHeight() / 2.0;
                        dialogStage.setX(centerX - dialogStage.getWidth() / 2.0);
                        dialogStage.setY(centerY - dialogStage.getHeight() / 2.0);
                    } else {
                        javafx.geometry.Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                        dialogStage.setX(screenBounds.getMinX() + (screenBounds.getWidth() - dialogStage.getWidth()) / 2.0);
                        dialogStage.setY(screenBounds.getMinY() + (screenBounds.getHeight() - dialogStage.getHeight()) / 2.0);
                    }
                });

                // Показываем модально и ждём выбора
                alert.showAndWait().ifPresentOrElse(choice -> {
                    if (choice == restartBtn) {
                        restartApplication();
                    } else if (choice == exitBtn) {
                        System.exit(0);
                    } else {
                        // Нажат "Отмена" — завершаем приложение
                        System.exit(0);
                    }
                }, () -> {
                    // Закрытие диалога через ESC или крестик — завершаем приложение
                    System.exit(0);
                });
                // Прерываем дальнейшую инициализацию, если врач не аутентифицирован
                return;
            }
        }*/
        tabsListView.setItems(FXCollections.observableArrayList("Вкладка 1", "Список пользователей", "Заявки на регистрацию"));
        tabsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) showViewForTab(newV);
        });
        tabsListView.getSelectionModel().selectFirst();

    }

    private void restartApplication() {     // ДОДЕЛАТЬ
    }


    private void showViewForTab(String tab) {
        try {
            Node view = viewCache.get(tab);
            if (view == null) {
                String fxml = switch (tab) {
                    case "Вкладка 1" -> "fxml/AnketsView.fxml";
                    case "Список пользователей" -> "fxml/usersView.fxml";
                    case "Заявки на регистрацию" -> "fxml/RegistrationRequestsView.fxml";
                    default -> "DefaultTab.fxml";
                };

                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                view = loader.load();


                viewCache.put(tab, view);
            }
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void attachHelp(Node node, String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.millis(250));
        t.setHideDelay(Duration.millis(100));
        node.setOnMouseEntered(e -> Tooltip.install(node, t));
        node.setOnMouseExited(e -> Tooltip.uninstall(node, t));
    }


}
