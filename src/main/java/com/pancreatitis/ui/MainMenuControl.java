package com.pancreatitis.ui;

import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainMenuControl {
    public ImageView userAvatar;
    @FXML private Label userNameLabel;
    @FXML private ListView<String> tabsListView;
    @FXML private StackPane contentPane;

    // кеш загруженных view для быстрого переключения
    private final Map<String, Node> viewCache = new HashMap<>();

    private static MainMenuControl instance;

    private FXMLLoader loader;

    static int idCurrentPatient;
    static int idCurrentQuestionnaire;
    static int idCurrentDoctor;
    static Patient currentPatient;
    static Questionnaire currentQuestionnaire;
    static Doctor currentDoctor;
    static boolean isAnketOpen;

    public static int deletedQuestionnaireId = -1;
    private final Map<String, Object> controllerCache = new HashMap<>();

    @FXML
    public void initialize() {
        idCurrentPatient = -1;
        idCurrentQuestionnaire = -1;
        idCurrentDoctor = User.getInstance().getId();
        currentPatient = new Patient();
        currentQuestionnaire = new Questionnaire();
        currentDoctor = User.getInstance().getDoctor();
        isAnketOpen = false;

        instance = this;
        tabsListView.setItems(FXCollections.observableArrayList("Список анкет", "Список пользователей", "Обучающая выборка", "Заявки на регистрацию", "Анкеты на верификацию"));
        tabsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) showViewForTab(newV);
        });
        tabsListView.getSelectionModel().selectFirst();

    }

    public static MainMenuControl getInstance() {
        return instance;
    }

    private static final Set<String> NO_CACHE_TABS = Set.of(
            "Анкета",
            "Список анкет",
            "Анкета обновления"
    );

    public void showViewForTab(String tab) {
        try {
            if (isAnketOpen && tab.equals("Список анкет")) {
                tab = "Анкета";
            }

            if (QuestionnaireViewUpdate.id != -1 && tab.equals("Анкеты на верификацию")) {
                tab = "Анкета обновления";
            }

            Node view = null;
            if (!NO_CACHE_TABS.contains(tab)) {
                view = viewCache.get(tab);
            }
            if (view == null) {
                String fxml = switch (tab) {
                    case "Список анкет" -> "fxml/QuestionListView.fxml";
                    case "Список пользователей" -> "fxml/usersView.fxml";
                    case "Заявки на регистрацию" -> "fxml/RegistrationRequestsView.fxml";
                    case "Обучающая выборка" -> "fxml/QuestionListTrainSet.fxml";
                    case "Анкеты на верификацию" -> "fxml/QuestionRequestsList.fxml";
                    case "Анкета" -> "fxml/QuestionCharacterView.fxml";
                    case "Анкета обновления" -> "fxml/QuestionnaireViewUpdate.fxml";
                    default -> "DefaultTab.fxml";
                };

                loader = new FXMLLoader(getClass().getResource(fxml));
                view = loader.load();

                controllerCache.put(tab, loader.getController());

                tabsListView.getSelectionModel().select(tab);
                viewCache.put(tab, view);
            }
            contentPane.getChildren().setAll(view);

            if ("Список анкет".equals(tab)) {
                QuestionListTableViewController listController = getControllerForTab(tab);
                if (listController != null) {
                    listController.checkForDeletedQuestionnaire();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void detachTabToNewWindow(String tabTitle) {
        try {
            Node view = viewCache.get(tabTitle);
            if (view == null) {
                // Если вкладка ещё не загружена — загрузим
                showViewForTab(tabTitle);
                view = viewCache.get(tabTitle);
            }

            if (view != null) {
                // Удаляем из текущего контейнера

                final Node finalView = view;
                contentPane.getChildren().remove(view);

                // Создаём новое окно
                Stage newStage = new Stage();
                newStage.setTitle(tabTitle + " (отдельное окно)");

                Scene newScene = new Scene((Parent) view);
                if (contentPane.getScene() != null) {
                    newScene.getStylesheets().addAll(contentPane.getScene().getStylesheets());
                }
                newStage.setScene( newScene );
                newStage.setOnCloseRequest(event -> {
                    Platform.runLater(() -> {
                        viewCache.put(tabTitle, finalView);
                        contentPane.getChildren().setAll(finalView);

                        // Опционально: переключиться на эту вкладку, если это TabPane
                        // tabsListView.getSelectionModel().select(finalTabTitle);
                    });
                });

                newStage.show();
            }
        } catch (Exception e) {
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


    public <T> T getTabController(){
        return loader.getController();
    }

    public static void openQuestionInNewWindow( Questionnaire quest ) {

    }

    @SuppressWarnings("unchecked")
    public <T> T getControllerForTab(String tab) {
        return (T) controllerCache.get(tab);
    }
}
