package com.pancreatitis.ui;

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

    // основная коллекция пациентов — ObservableMap<id, Patient>
    //private ObservableMap<Integer, Patient> patientMap = FXCollections.observableHashMap();


    @FXML
    private void initialize() {
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        List<QuestionnaireItem> allQuestionnaireItems = databaseModule.getAllQuestionnaireItems();
        for(QuestionnaireItem item: allQuestionnaireItems){
            rows.add(item);
        }

        TrainSetModule trainSetModule = TrainSetModule.getInstance();
        trainSetModule.load();

        colNamePerson.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFioPatient()));
        colDiagnosis.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDiagnosis()));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDateOfCompletion()));
//        colAction.setCellFactory(param -> new TableCell<>() {
//            private final Button approveBtn = createButton("✅", "approve");
//            private final Button rejectBtn = createButton("❌", "reject");
//            private final HBox container = new HBox(5, approveBtn, rejectBtn);
//
//            {
//                container.setAlignment(javafx.geometry.Pos.CENTER);
//                container.setPadding(new Insets(2));
//            }
//
//            @Override
//            protected void updateItem(Void item, boolean empty) {
//                super.updateItem(item, empty);
//                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
//                    setGraphic(null);
//                } else {
//                    RegistrationForm form = getTableRow().getItem();
//                    approveBtn.setOnAction(e -> handleApprove(form));
//                    rejectBtn.setOnAction(e -> handleReject(form));
//                    setGraphic(container);
//                }
//            }
//        });

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

    private void handleAdd(RegistrationForm form) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтвердить одобрение");
        confirm.setHeaderText("Одобрить заявку пользователя " + form.getLogin() + "?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {

        }
    }

    private void handleRemove(RegistrationForm form) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Отклонение заявки");
        confirm.setHeaderText("Отклонить заявку пользователя " + form.getLogin() + "?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {

        }
    }
}
