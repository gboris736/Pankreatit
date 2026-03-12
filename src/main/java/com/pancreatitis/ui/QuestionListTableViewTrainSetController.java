package com.pancreatitis.ui;

import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.QuestionnaireItem;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.registration.RegistrationModule;
import com.pancreatitis.modules.trainset.TrainSetModule;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;

import java.util.List;

public class QuestionListTableViewTrainSetController {

    @FXML private TableView<QuestionnaireItem> tableViewQuestion;
    @FXML private TableColumn<QuestionnaireItem, String> colNamePerson;
    @FXML private TableColumn<QuestionnaireItem, String> colDate;
    @FXML private TableColumn<QuestionnaireItem, String> colDiagnosis;
    @FXML private TableColumn<QuestionnaireItem, String> colAction;

    // Добавленные FXML-элементы для поиска
    @FXML private TextField searchField;      // id="searchField" в FXML
    @FXML private Label countLabel;          // необязательно, id="countLabel"

    // таблица отображает список анкет
    private final ObservableList<QuestionnaireItem> rows = FXCollections.observableArrayList();
    private FilteredList<QuestionnaireItem> filteredRows;

    TrainSetModule trainSetModule = TrainSetModule.getInstance();


    @FXML
    private void initialize() {
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        List<QuestionnaireItem> allQuestionnaireItems = databaseModule.getAllQuestionnaireItems();
        for(QuestionnaireItem item: allQuestionnaireItems){
            rows.add(item);
        }

        trainSetModule.load();

        colNamePerson.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFioPatient()));
        colDiagnosis.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDiagnosis()));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDateOfCompletion()));
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button approveBtn = createButton("✅", "approve");
            private final Button rejectBtn = createButton("❌", "reject");
            private final HBox container = new HBox(5, approveBtn, rejectBtn);

            {
                container.setAlignment(javafx.geometry.Pos.CENTER);
                container.setPadding(new Insets(2));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    approveBtn.setOnAction(e -> handleAdd(form));
                    rejectBtn.setOnAction(e -> handleRemove(form));
                    setGraphic(container);
                }
            }
        });

        HelpUtils.attachHelp(colNamePerson, "ФИО пациента");
        HelpUtils.attachHelp(colDate, "Дата создания анкеты");
        HelpUtils.attachHelp(colDiagnosis, "Поставленный в анкете");

        // Используем FilteredList поверх rows для поиска
        filteredRows = new FilteredList<>(rows, p -> true);
        tableViewQuestion.setItems(filteredRows);

        colDate.setSortType(TableColumn.SortType.DESCENDING);
        tableViewQuestion.getSortOrder().add(colDate);

        // привязка обработчика для поля поиска
        if (searchField != null) {
            searchField.setPromptText("Поиск: фио пациента или врач...");
            searchField.textProperty().addListener((obs, oldV, newV) -> {
                String query = newV == null ? "" : newV.trim().toLowerCase();
                filteredRows.setPredicate(row -> {
                    if (query.isEmpty()) return true;
                    // поиск по ФИО пациента, ФИО врача и месту поступления
                    boolean inPatient = containsIgnoreCase(row.getFioPatient(), query);
                    boolean inAdmit = containsIgnoreCase(row.getDiagnosis(), query);
                    return inPatient || inAdmit;
                });
                updateCountLabel();
            });
        }

        // первичная загрузка в rows
        //rebuildRowsFromMap();

        // обновим счётчик при старте
        updateCountLabel();
    }

    private boolean containsIgnoreCase(String source, String query) {
        if (source == null) return false;
        return source.toLowerCase().contains(query);
    }

    private void updateCountLabel() {
        if (countLabel != null) {
            int size = filteredRows == null ? rows.size() : filteredRows.size();
            countLabel.setText("Найдено: " + size);
        }
    }

    private void handleAdd(Questionnaire questionnaire) {

        trainSetModule.addQuestionnaire(questionnaire.);
    }

    private void handleRemove(Questionnaire questionnaire) {
        trainSetModule.deleteQuestionnaire(questionnaire);
    }


    private Button createButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.setMinWidth(30);
        btn.getStyleClass().add("table-button");
        btn.getStyleClass().add("table-button-" + styleClass);
        btn.setOnAction(e -> e.consume()); // Предотвращает снятие выделения строки
        return btn;
    }
}
