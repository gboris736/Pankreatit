package com.pancreatitis.ui;

import com.pancreatitis.models.QuestionnaireItem;
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
    @FXML private TableColumn<QuestionnaireItem, LocalDate> colDate;
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

        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setCellFactory(col -> new TableCell<QuestionnaireItem, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(DateTimeFormatter.ofPattern("dd.MM.yyyy").format(item));
                }
            }
        });

        generateTestData(26);


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



/*
        //Автоматическое обновление данных было
        patientMap = Main.DATA_CONTROLLER.getPatientMap();

        // слушаем изменения в patientMap и синхронизируем rows
        patientMap.addListener((MapChangeListener<Integer, Patient>) change -> {
            if (change.wasRemoved() && !change.wasAdded()) {
                Patient removed = change.getValueRemoved();
                if (removed != null) removeRowsForPatient(removed);
            }
            if (change.wasAdded() && !change.wasRemoved()) {
                Patient added = change.getValueAdded();
                if (added != null) addRowsForPatient(added);
            }
            if (change.wasAdded() && change.wasRemoved()) {
                // замена объекта по ключу
                Patient removed = change.getValueRemoved();
                Patient added = change.getValueAdded();
                if (removed != null) removeRowsForPatient(removed);
                if (added != null) addRowsForPatient(added);
            }
        });
*/
        // привязка обработчика для поля поиска
        if (searchField != null) {
            searchField.setPromptText("Поиск: фио пациента, врач или место поступления...");
            searchField.textProperty().addListener((obs, oldV, newV) -> {
                String query = newV == null ? "" : newV.trim().toLowerCase();
                filteredRows.setPredicate(row -> {
                    if (query.isEmpty()) return true;
                    // поиск по ФИО пациента, ФИО врача и месту поступления
                    boolean inPatient = containsIgnoreCase(row.getFioPatient(), query);
                    boolean inDoctor = containsIgnoreCase(row.getData().toString(), query);
                    boolean inAdmit = containsIgnoreCase(row.getDiagnosis(), query);
                    return inPatient || inDoctor || inAdmit;
                });
                updateCountLabel();
            });
        }

        // первичная загрузка в rows
        //rebuildRowsFromMap();

        // обновим счётчик при старте
        updateCountLabel();
    }

    void generateTestData(int count){
        List<QuestionnaireItem> items = new ArrayList<>();
        Random random = new Random();

        // Наборы данных для вариативности
        String[] firstNames = {"Иван", "Петр", "Сергей", "Анна", "Мария", "Ольга", "Дмитрий", "Алексей"};
        String[] lastNames = {"Иванов", "Петров", "Сидоров", "Смирнов", "Кузнецов", "Попов", "Васильев"};
        String[] diagnoses = {"1", "5", "6"}; // Коды МКБ-10

        for (int i = 0; i < count; i++) {
            QuestionnaireItem item = new QuestionnaireItem();

            // Генерация ФИО
            String fio = lastNames[random.nextInt(lastNames.length)] + " " +
                    firstNames[random.nextInt(firstNames.length)] + " " +
                    (char)('A' + random.nextInt(26)) + ".";

            // Генерация даты (случайная дата за последние 30 дней)
            LocalDate date = LocalDate.now().minusDays(random.nextInt(30));

            item.setIdQuestionnaire(100 + i); // Уникальный ID анкеты
            item.setFioPatient(fio);
            item.setDiagnosis(diagnoses[random.nextInt(diagnoses.length)]);
            item.setData(date);

            items.add(item);
        }
        rows.addAll(items);
    }


    /*// rebuild: всё выстроить заново (например после массового обновления)
    private void rebuildRowsFromMap() {
        rows.clear();
        for (Map.Entry<Integer, Patient> e : patientMap.entrySet()) {
            Patient p = e.getValue();
            if (p == null) continue;
            List<Anket> ankets = p.getAnketList();
            if (ankets == null) continue;
            for (Anket a : ankets) rows.add(anketToRow(a));
        }
    }*/


    private String safeFio(String s) { return s == null ? "" : s; }

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

            long idQuestionnaire = item.getIdQuestionnaire();
            /*
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("fxml/QuestionCharacterView.fxml"));
            Parent root = loader.load();

            AnketCharViewController ctrl = loader.getController();
            Doctor currentDoctor = Main.DATA_CONTROLLER.getCurrentDoctor();

            ctrl.setDoctor(currentDoctor);
            ctrl.setAnket(anket);
            ctrl.applyDataBindings();

            Stage stage = new Stage();
            stage.setTitle("Анкета: " + safeFio(anket.getPatient().getFioName()));
            stage.initModality(Modality.NONE);
            stage.setScene(new Scene(root));

            // 🔒 Добавляем обработчик закрытия окна
            stage.setOnCloseRequest(event -> {
                if (!ctrl.requestClose()) {
                    // Если requestClose() вернул false — отменяем закрытие
                    event.consume();
                }
                // Если вернул true — окно закроется автоматически
            });

            stage.show();
            */
        } catch (Exception e) {
            showAlert("Не удалось открыть окно анкеты: " + e.getMessage());
            e.printStackTrace();
        }
    }


//    public static class AnketRow {
//        private final SimpleStringProperty patientFio;
//        private final SimpleStringProperty doctorFio;
//        private final SimpleStringProperty admittedFrom;
//        private final LocalDate date;
//        private final Anket anket;
//
//        public AnketRow(String patientFio, String doctorFio, LocalDate date, Anket anket) {
//            this.admittedFrom = new SimpleStringProperty(anket.getAdmittedFrom());
//            this.patientFio = new SimpleStringProperty(patientFio);
//            this.doctorFio = new SimpleStringProperty(doctorFio);
//            this.date = date;
//            this.anket = anket;
//        }
//
//        public LocalDate getDate() {
//            return date;
//        }
//
//
//        public SimpleStringProperty admittedFromProperty() {
//            return admittedFrom;
//        }
//
//        public String getAdmittedFrom() { return admittedFrom.get(); }
//        public String getPatientFio() { return patientFio.get(); }
//        public String getDoctorFio() { return doctorFio.get(); }
//        public Anket getAnket() { return anket; }
//    }
}
