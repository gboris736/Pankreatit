package com.pancreatitis.ui;

import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.QuestionnaireItem;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.database.DatabaseModule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;

import java.util.List;

public class QuestionListTableViewController {

    @FXML private TableView<QuestionnaireItem> tableViewQuestion;
    @FXML private TableColumn<QuestionnaireItem, String> colNamePerson;
    @FXML private TableColumn<QuestionnaireItem, String> colDate;
    @FXML private TableColumn<QuestionnaireItem, String> colDiagnosis;
    @FXML private TableColumn<QuestionnaireItem, String> colDoctor;
    @FXML private TextField searchField;
    @FXML private Label countLabel;

    private final ObservableList<QuestionnaireItem> rows = FXCollections.observableArrayList();
    private FilteredList<QuestionnaireItem> filteredRows;

    @FXML
    private void createNewQuestionnaire() {
        MainMenuControl mainMenuControl = MainMenuControl.getInstance();
        MainMenuControl.idCurrentPatient = -1;
        MainMenuControl.currentPatient = new Patient();
        MainMenuControl.idCurrentQuestionnaire = -1;
        MainMenuControl.currentQuestionnaire = new Questionnaire();
        MainMenuControl.idCurrentDoctor = User.getInstance().getId();
        MainMenuControl.currentDoctor = User.getInstance().getDoctor();
        MainMenuControl.isAnketOpen = true;
        mainMenuControl.showViewForTab("Анкета");
    }

    @FXML
    private void initialize() {
        colNamePerson.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFioPatient()));
        colDiagnosis.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDiagnosis()));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDateOfCompletion()));
        colDoctor.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFioDoctor()));

        // Загрузка данных из БД
        loadDataFromDB();

        // Настройка двойного клика для открытия анкеты
        tableViewQuestion.setRowFactory(tv -> {
            TableRow<QuestionnaireItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    QuestionnaireItem clicked = row.getItem();
                    openQuestionnaireDetail(clicked);
                }
            });
            return row;
        });

        // Подсказки
        HelpUtils.attachHelp(colNamePerson, "ФИО пациента");
        HelpUtils.attachHelp(colDate, "Дата создания анкеты");
        HelpUtils.attachHelp(colDiagnosis, "Поставленный в анкете");
        HelpUtils.attachHelp(colDoctor, "Ответственный врач");

        // Фильтруемый список
        filteredRows = new FilteredList<>(rows, p -> true);
        tableViewQuestion.setItems(filteredRows);

        // Сортировка по дате (по убыванию)
        colDate.setSortType(TableColumn.SortType.DESCENDING);
        tableViewQuestion.getSortOrder().add(colDate);

        // Поиск
        searchField.setPromptText("Поиск: фио пациента, врач или место поступления...");
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            String query = newV == null ? "" : newV.trim().toLowerCase();
            filteredRows.setPredicate(row -> {
                if (query.isEmpty()) return true;
                boolean inPatient = containsIgnoreCase(row.getFioPatient(), query);
                boolean inDiagnosis = containsIgnoreCase(row.getDiagnosis(), query);
                boolean inDoctor = containsIgnoreCase(row.getFioDoctor(), query);
                return inPatient || inDiagnosis || inDoctor;
            });
            updateCountLabel();
        });

        updateCountLabel();
    }

    /** Загружает данные из БД в ObservableList (вызывается только при инициализации) */
    private void loadDataFromDB() {
        rows.clear();
        DatabaseModule db = DatabaseModule.getInstance();
        List<QuestionnaireItem> items = db.getAllQuestionnaireItems();
        rows.addAll(items);
    }

    /** Проверяет, была ли удалена анкета, и если да, удаляет её из списка */
    public void checkForDeletedQuestionnaire() {
        int deletedId = MainMenuControl.deletedQuestionnaireId;
        if (deletedId != -1) {
            removeQuestionnaireFromList(deletedId);
            MainMenuControl.deletedQuestionnaireId = -1; // сброс
        }
    }

    /** Удаляет элемент с заданным ID из ObservableList (без перезагрузки всей таблицы) */
    private void removeQuestionnaireFromList(int id) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getIdQuestionnaire() == id) {
                rows.remove(i);
                updateCountLabel(); // обновляем счётчик
                break;
            }
        }
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
            DatabaseModule db = DatabaseModule.getInstance();
            MainMenuControl.idCurrentQuestionnaire = item.getIdQuestionnaire();
            MainMenuControl.idCurrentPatient = item.getIdPatient();
            MainMenuControl.idCurrentDoctor = item.getIdDoctor();
            MainMenuControl.currentPatient = db.getPatientById(item.getIdPatient());
            MainMenuControl.currentQuestionnaire = db.getQuestionnaireById(item.getIdQuestionnaire());
            MainMenuControl.currentDoctor = db.getDoctorById(item.getIdDoctor());
            MainMenuControl.isAnketOpen = true;

            MainMenuControl mainMenuControl = MainMenuControl.getInstance();
            mainMenuControl.showViewForTab("Анкета");
        } catch (Exception e) {
            showAlert("Не удалось открыть анкету: " + e.getMessage());
            e.printStackTrace();
        }
    }
}