package com.pancreatitis.modules.database;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.localstorage.DiskStorageControl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DatabaseModule {
    private Path dbPath;
    private String url;
    private static DatabaseModule instance;
    private DiskStorageControl diskStorageControl;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized DatabaseModule getInstance() {
        if (instance == null) {
            instance = new DatabaseModule();
        }
        return instance;
    }

    private DatabaseModule() {
        diskStorageControl = DiskStorageControl.getInstance();
        dbPath = diskStorageControl.getDBPath();

        if (!Files.exists(dbPath)) {
            throw new IllegalArgumentException("Database file not found: " + dbPath);
        } else {
            url = "jdbc:sqlite:" + dbPath.toString();
            System.out.println("Database URL: " + url);
        }
    }

    // ========== Characteristic ==========
    public Characteristic getCharacteristicById(int id) {
        String sql = "SELECT * FROM characteristic WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Characteristic characteristic = new Characteristic();
                characteristic.setId(rs.getInt("id"));
                characteristic.setName(rs.getString("name"));
                characteristic.setOpis(rs.getString("opis"));
                characteristic.setIdType(rs.getInt("id_type"));
                characteristic.setHints(rs.getString("hints"));
                characteristic.setWeight(rs.getFloat("weight"));
                characteristic.setMinValue(rs.getInt("min_value"));
                characteristic.setMaxValue(rs.getInt("max_value"));
                characteristic.setLastModified(rs.getString("last_modified"));
                return characteristic;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Characteristic> getAllCharacteristics() {
        List<Characteristic> characteristics = new ArrayList<>();
        String sql = "SELECT * FROM characteristic ORDER BY id";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Characteristic characteristic = new Characteristic();
                characteristic.setId(rs.getInt("id"));
                characteristic.setName(rs.getString("name"));
                characteristic.setOpis(rs.getString("opis"));
                characteristic.setIdType(rs.getInt("id_type"));
                characteristic.setHints(rs.getString("hints"));
                characteristic.setWeight(rs.getFloat("weight"));
                characteristic.setMinValue(rs.getInt("min_value"));
                characteristic.setMaxValue(rs.getInt("max_value"));
                characteristic.setLastModified(rs.getString("last_modified"));
                characteristics.add(characteristic);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return characteristics;
    }

    // ========== Doctor ==========
    public Doctor getDoctorById(int id) {
        String sql = "SELECT * FROM doctors WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Doctor doctor = new Doctor();
                doctor.setId(rs.getInt("id"));
                doctor.setFio(rs.getString("fio"));
                doctor.setStatus(rs.getInt("status") == 1);
                doctor.setLastModified(rs.getString("last_modified"));
                return doctor;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long insertDoctor(Doctor doctor) {
        String sql = "INSERT INTO doctors (fio, status, last_modified) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, doctor.getFio());
            pstmt.setInt(2, doctor.getStatus() ? 1 : 0);
            pstmt.setString(3, LocalDateTime.now().format(formatter));

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public Doctor getDoctorByFio(String fio) {
        String sql = "SELECT * FROM doctors WHERE fio = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fio);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Doctor doctor = new Doctor();
                doctor.setId(rs.getInt("id"));
                doctor.setFio(rs.getString("fio"));
                doctor.setStatus(rs.getInt("status") == 1);
                return doctor;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Doctor> getDoctorList() {
        List<Doctor> doctors = new ArrayList<>();
        String sql = "SELECT * FROM doctors ORDER BY id";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Doctor doctor = new Doctor();
                doctor.setId(rs.getInt("id"));
                doctor.setFio(rs.getString("fio"));
                doctor.setStatus(rs.getInt("status") == 1);
                doctors.add(doctor);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return doctors;
    }

    // ========== Patient ==========
    public Patient getPatientById(int id) {
        String sql = "SELECT * FROM patients WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Patient patient = new Patient();
                patient.setId(rs.getInt("id"));
                patient.setFio(rs.getString("fio"));
                patient.setLastModified(rs.getString("last_modified"));
                return patient;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long insertPatient(Patient patient) {
        String sql = "INSERT INTO patients (fio, last_modified) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, patient.getFio());
            pstmt.setString(2, LocalDateTime.now().format(formatter));

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<Patient> getPatientList() {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM patients ORDER BY id";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Patient patient = new Patient();
                patient.setId(rs.getInt("id"));
                patient.setFio(rs.getString("fio"));
                patient.setLastModified(rs.getString("last_modified"));
                patients.add(patient);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return patients;
    }

    // ========== Questionnaire ==========
    public long insertQuestionnaire(Questionnaire questionnaire) {
        String sql = "INSERT INTO ankets (id_patient, id_doctor, id_expert, diagnosis, admitted_from, date_of_completion, last_modified) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, (int)questionnaire.getIdPatient());
            pstmt.setInt(2, questionnaire.getIdDoctor());
            pstmt.setInt(3, questionnaire.getIdExpert());
            pstmt.setString(4, questionnaire.getDiagnosis());
            pstmt.setString(5, questionnaire.getAdmittedFrom());
            pstmt.setString(6, LocalDateTime.now().format(formatter));
            pstmt.setString(7, LocalDateTime.now().format(formatter));

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int updateQuestionnaire(Questionnaire questionnaire) {
        String sql = "UPDATE ankets SET diagnosis = ?, admitted_from = ?, last_modified = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, questionnaire.getDiagnosis());
            pstmt.setString(2, questionnaire.getAdmittedFrom());
            pstmt.setString(3, LocalDateTime.now().format(formatter));
            pstmt.setInt(4, (int)questionnaire.getId());

            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Questionnaire getQuestionnaireById(int id) {
        String sql = "SELECT * FROM ankets WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Questionnaire questionnaire = new Questionnaire();
                questionnaire.setId(rs.getInt("id"));
                questionnaire.setIdPatient(rs.getInt("id_patient"));
                questionnaire.setIdDoctor(rs.getInt("id_doctor"));
                questionnaire.setIdExpert(rs.getInt("id_expert"));
                questionnaire.setDiagnosis(rs.getString("diagnosis"));
                questionnaire.setAdmittedFrom(rs.getString("admitted_from"));
                questionnaire.setDateOfCompletion(rs.getString("date_of_completion"));
                questionnaire.setLastModified(rs.getString("last_modified"));
                return questionnaire;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QuestionnaireItem> getAllQuestionnaireItems() {
        List<QuestionnaireItem> items = new ArrayList<>();
        String sql = "SELECT ankets.id as idQuestionnaire, patients.id as idPatient, patients.fio as fioPatient, ankets.diagnosis as diagnosis, ankets.date_of_completion as dateOfCompletion " +
                "FROM ankets JOIN patients ON ankets.id_patient = patients.id " +
                "ORDER BY patients.fio";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                QuestionnaireItem item = new QuestionnaireItem();
                item.setIdQuestionnaire(rs.getInt("idQuestionnaire"));
                item.setIdPatient(rs.getInt("idPatient"));
                item.setFioPatient(rs.getString("fioPatient"));
                item.setDiagnosis(rs.getString("diagnosis"));
                item.setDateOfCompletion(rs.getString("dateOfCompletion"));
                items.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    private List<Questionnaire> getQuestionnairesForPatient(int patientId) {
        List<Questionnaire> questionnaires = new ArrayList<>();
        String sql = "SELECT * FROM ankets WHERE id_patient = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, patientId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Questionnaire questionnaire = new Questionnaire();
                questionnaire.setId(rs.getInt("id"));
                questionnaire.setIdPatient(rs.getInt("id_patient"));
                questionnaire.setIdDoctor(rs.getInt("id_doctor"));
                questionnaire.setIdExpert(rs.getInt("id_expert"));
                questionnaire.setDiagnosis(rs.getString("diagnosis"));
                questionnaire.setAdmittedFrom(rs.getString("admitted_from"));
                questionnaire.setDateOfCompletion(rs.getString("date_of_completion"));
                questionnaire.setLastModified(rs.getString("last_modified"));
                questionnaires.add(questionnaire);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return questionnaires;
    }

    // ========== CharacterizationAnketPatient ==========
    public void insertAllCharacterizationAnketPatient(List<CharacterizationAnketPatient> characterizations) {
        String sql = "INSERT INTO characterization_anket_patient (id_characteristic, id_anket, id_value, value, created_at, last_modified) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (CharacterizationAnketPatient cap : characterizations) {
                pstmt.setInt(1, cap.getIdCharacteristic());
                pstmt.setInt(2, (int)cap.getIdAnket());

                if (cap.getIdValue() != 0) {
                    pstmt.setInt(3, cap.getIdValue());
                } else {
                    pstmt.setNull(3, Types.INTEGER);
                }

                if (cap.getValue() != -1) {
                    pstmt.setFloat(4, cap.getValue());
                } else {
                    pstmt.setNull(4, Types.FLOAT);
                }

                pstmt.setString(5, cap.getCreatedAt() != null ? cap.getCreatedAt() : LocalDateTime.now().format(formatter));
                pstmt.setString(6, LocalDateTime.now().format(formatter));

                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public long insertCharacterizationAnketPatient(CharacterizationAnketPatient characterization) {
        String sql = "INSERT INTO characterization_anket_patient (id_characteristic, id_anket, id_value, value, created_at, last_modified) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, characterization.getIdCharacteristic());
            pstmt.setInt(2, (int)characterization.getIdAnket());

            if (characterization.getIdValue() != 0) {
                pstmt.setInt(3, characterization.getIdValue());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }

            if (characterization.getValue() != -1) {
                pstmt.setFloat(4, characterization.getValue());
            } else {
                pstmt.setNull(4, Types.FLOAT);
            }

            pstmt.setString(5, characterization.getCreatedAt() != null ? characterization.getCreatedAt() : LocalDateTime.now().format(formatter));
            pstmt.setString(6, LocalDateTime.now().format(formatter));

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int updateCharacterizationAnketPatient(CharacterizationAnketPatient characterization) {
        String sql = "UPDATE characterization_anket_patient SET id_value = ?, value = ?, last_modified = ? WHERE id_anket = ? AND id_characteristic = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, characterization.getIdValue());
            pstmt.setFloat(2, characterization.getValue());
            pstmt.setString(3, LocalDateTime.now().format(formatter));
            pstmt.setInt(4, (int)characterization.getIdAnket());
            pstmt.setInt(5, characterization.getIdCharacteristic());

            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<CharacterizationAnketPatient> getCharacterizationsForAnket(int anketId) {
        List<CharacterizationAnketPatient> characterizations = new ArrayList<>();
        String sql = "SELECT * FROM characterization_anket_patient WHERE id_anket = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, anketId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                CharacterizationAnketPatient cap = new CharacterizationAnketPatient();
                cap.setIdAnket(rs.getInt("id_anket"));
                cap.setIdCharacteristic(rs.getInt("id_characteristic"));
                cap.setCreatedAt(rs.getString("created_at"));
                cap.setLastModified(rs.getString("last_modified"));

                int idValue = rs.getInt("id_value");
                if (!rs.wasNull()) {
                    cap.setIdValue(idValue);
                }

                float value = rs.getFloat("value");
                if (!rs.wasNull()) {
                    cap.setValue(value);
                }

                characterizations.add(cap);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return characterizations;
    }

    // ========== QuestionnaireValue ==========
    public List<CharacterizationValue> getValuesForCharacteristic(int characteristicId) {
        List<CharacterizationValue> values = new ArrayList<>();
        String sql = "SELECT * FROM ankets_values WHERE id_characteristic = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, characteristicId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                CharacterizationValue value = new CharacterizationValue();
                value.setId(rs.getInt("id"));
                value.setIdCharacteristic(rs.getInt("id_characteristic"));
                value.setIdValue(rs.getInt("id_value"));
                value.setValue(rs.getString("value"));
                value.setLastModified(rs.getString("last_modified"));
                values.add(value);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return values;
    }

    // ========== Transaction methods ==========
    public void beginTransaction() {
        try {
            Connection conn = DriverManager.getConnection(url);
            conn.setAutoCommit(false);
            // В реальном приложении нужно хранить соединение для транзакции
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setTransactionSuccessful() {
        // В реальном приложении нужно закоммитить транзакцию
    }

    public void endTransaction() {
        // В реальном приложении нужно закрыть соединение или откатить если не было успеха
    }
}