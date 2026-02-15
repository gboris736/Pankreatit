package com.pancreatitis.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.collections.FXCollections;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainMenuControl {
    @FXML private Label userNameLabel;
    @FXML private ListView<String> tabsListView;
    @FXML private StackPane contentPane;
    //private final String absAdressFxmlFile = "resources.io.pankr.";

    // кеш загруженных view для быстрого переключения
    private final Map<String, Node> viewCache = new HashMap<>();

    @FXML
    public void initialize() {

    }

    private void restartApplication() {     // ДОДЕЛАТЬ
    }


    private void showViewForTab(String tab) {
        try {
            Node view = viewCache.get(tab);
            if (view == null) {
                String fxml = switch (tab) {
                    case "Вкладка 1" -> "fxml/AnketsView.fxml";
                    case "Вкладка 2" -> "fxml/CreateAnket.fxml";
                    default -> "DefaultTab.fxml";
                };

                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                view = loader.load();

                if ("Вкладка 1".equals(tab)) {
                }

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
