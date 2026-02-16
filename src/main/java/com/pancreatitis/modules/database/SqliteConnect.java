package com.pancreatitis.modules.database;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.localstorage.DiskStorageControl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;


//String url = "jdbc:sqlite:/home/core2quad/Documents/IVCHT/project/Pankreat/DB/pancreatitis.db";
/*
"""
            SELECT
                p.id AS id_patient,
                p.fio AS fio_patient,
                c.id_characteristic AS id_char
            FROM patients p
            LEFT JOIN characterization_patient c ON p.id = c.id_patient
            ORDER BY p.id;
            """
 */


public class SqliteConnect {
    private Path dbPath;
    private String url;
    private static SqliteConnect instance;
    private ModelDataControl modelDataControl;
    private DiskStorageControl diskStorageControl;

    public static synchronized SqliteConnect getInstance(){
        if (instance == null){
            instance = new SqliteConnect();
        }
        return instance;
    }



    private SqliteConnect() {
        modelDataControl = ModelDataControl.getInstance();
        diskStorageControl = DiskStorageControl.getInstance();
        dbPath = diskStorageControl.getPathLibrary().get("aapDBPath");

        if (!Files.exists(dbPath)) {
            throw new IllegalArgumentException(url + " must not be null or empty");
        }
        else{
            url = "jdbc:sqlite:" + dbPath.toString();
            System.out.print(url);
        }
    }

    public static boolean isConnection(String url) {
        try (Connection conn = DriverManager.getConnection(url)) {
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    public boolean isConnection() {
        try (Connection conn = DriverManager.getConnection(url)) {
        } catch (SQLException e) {
            return false;
        }
        return true;
    }


    public ArrayList<Patient> getPatientList() throws SQLException {
        String sqlPat = """
                SELECT
                    p.id AS id_patient,
                    p.fio AS fio_patient
                FROM patients p
                ORDER BY p.id;
                """;

        ArrayList<Patient> patients = new ArrayList<>();
        try (var conn = DriverManager.getConnection(url);
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sqlPat)) {

            while (rs.next()) {
                int id = rs.getInt("id_patient");
                String patientFio = rs.getString("fio_patient");

                Patient tempP = new Patient(
                        patientFio,
                        id
                );

                tempP.setAnketList(getAnketList (id, tempP));
                patients.add(tempP);
            }
        }
        return patients;
    }


    public ArrayList<Doctor> getDoctorList()  {
        String sql = """
            SELECT
                d.id AS id_doctor,
                d.fio AS fio_doctor,
                d.status AS status_doctor
            FROM doctors d
            ORDER BY d.id;
            """;

        ArrayList<Doctor> doctors = new ArrayList<>();
        try (var conn = DriverManager.getConnection(url);
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id_doctor");                   // было "id_patient"
                boolean doctorStatus = rs.getBoolean("status_doctor");
                String doctorFio = rs.getString("fio_doctor");     // было "fio_patient"
                doctors.add(new Doctor(doctorFio, id, doctorStatus));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return doctors;
    }


    ArrayList<Anket> getAnketList(int idPatient, Patient patient) throws SQLException {

        ArrayList<Anket> anketsRet = new ArrayList<>();
        String sqlAnk = String.format("""
                SELECT
                    id,
                    id_patient,
                    id_doctor,
                    id_expert,
                    diagnosis,
                    date_of_completion,
                    admitted_from
                FROM ankets
                WHERE id = %d;
                """, idPatient);
        try (var conn = DriverManager.getConnection(url);
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sqlAnk)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                int idDoc = rs.getInt("id_doctor");
                int idExp = rs.getInt("id_doctor");
                String diagn = rs.getString("diagnosis");
                LocalDate dateCompl = rs.getDate("date_of_completion").toLocalDate();
                String admittedFrom = rs.getString("admitted_from");

                Anket addAnket =new Anket(
                        modelDataControl.getDoctorMap().get(idDoc),
                        patient,
                        new ArrayList<>(),
                        id,
                        diagn,
                        dateCompl,
                        admittedFrom
                );
                addAnket.addCharacters(modelDataControl.getDoctorMap().get(idDoc), getCharAnketList(addAnket));

                anketsRet.add(addAnket);


            }
        }
        return anketsRet;

    }



    ArrayList<AnketCharacter> getCharAnketList(Anket anket) throws SQLException {

        ArrayList<AnketCharacter> characterListRet = new ArrayList<>();
        String sqlAnk = String.format("""
                SELECT
                    id_characteristic,
                    value
                FROM characterization_anket_patient
                WHERE id_anket = %d ORDER BY id_characteristic;
                """, anket.getId());
        try (var conn = DriverManager.getConnection(url);
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sqlAnk)) {

            while (rs.next()) {
                int id_characteristic = rs.getInt("id_characteristic");
                int value = rs.getInt("value");
                characterListRet.add(new AnketCharacter(anket,
                        modelDataControl.getAnketCharacterTypeMap().get(id_characteristic),
                        value
                ));
            }
        }
        return characterListRet;

    }


    public ArrayList<AnketCharacter.AnketCharacterType> getAllCharacteristicType() throws SQLException {
        ArrayList<AnketCharacter.AnketCharacterType> list = new ArrayList<>();
        String sql = "SELECT id, name, opis, type, hints, weight, min_value, max_value FROM characteristic ORDER BY id;";
        try (var conn = DriverManager.getConnection(url);
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                AnketCharacter.AnketCharacterType ch = AnketCharacter.AnketCharacterType.fromResultSet(rs);
                list.add(ch);
            }
        }
        return list;
    }


    public static void main(String[] args) throws SQLException {
        String urlTest = "jdbc:sqlite:/home/core2quad/.local/share/pankreatmanager/dataBaseStor/pancreatitis_v5.db";
        Connection conn = DriverManager.getConnection(urlTest);
    }
}
