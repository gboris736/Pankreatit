package com.pancreatitis.ui;

import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.QuestionnaireItem;
import com.pancreatitis.modules.database.DatabaseModule;
import com.sun.tools.javac.Main;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuestionListTableViewController {

    @FXML private TableView<QuestionnaireItem> tableViewQuestion;
    @FXML private TableColumn<QuestionnaireItem, String> colNamePerson;
    @FXML private TableColumn<QuestionnaireItem, String> colDate;
    @FXML private TableColumn<QuestionnaireItem, String> colDiagnosis;

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
        colNamePerson.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFioPatient()));
        colDiagnosis.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDiagnosis()));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDateOfCompletion()));

        DatabaseModule databaseModule = DatabaseModule.getInstance();
        List<QuestionnaireItem> allQuestionnaireItems = databaseModule.getAllQuestionnaireItems();
        for(QuestionnaireItem item: allQuestionnaireItems){
            rows.add(item);
        }

        // Открытие анкеты напиши полу
        tableViewQuestion.setRowFactory(tv -> {
            TableRow<QuestionnaireItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2
                        && (!row.isEmpty())) {
                    QuestionnaireItem clicked = row.getItem();
                    openQuestionnaireDetail(clicked);       //НАПИШИ свой вызов функции открытия
                }
            });
            return row;
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
            searchField.setPromptText("Поиск: фио пациента, врач или место поступления...");
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

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.showAndWait();
    }


    private void openQuestionnaireDetail(QuestionnaireItem item) {
        try {
            System.out.println(item.getIdQuestionnaire());

            MainMenuControl mainMenuControl = MainMenuControl.getInstance();
            mainMenuControl.showViewForTab("Анкета");

        } catch (Exception e) {
            showAlert("Не удалось открыть анкету: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
