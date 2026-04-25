package com.pancreatitis.models;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class QuestionnaireItem {
    private int idQuestionnaire;
    private int idPatient;
    private int idDoctor;
    private String fioPatient;
    private String diagnosis = "-";
    private String dateOfCompletion;
    private String fioDoctor;

    public QuestionnaireItem() {}

    private String codeToDiagnosis(String code){
        Map<String, String> diagnosisMap = new HashMap<>();
        diagnosisMap.put("1", "Отечный панкреатит");
        diagnosisMap.put("5", "Панкреонекроз среднетяжелое течение");
        diagnosisMap.put("6", "Панкреонекроз тяжелое течение");
        return diagnosisMap.getOrDefault(code, "-");
    }

    public int getIdQuestionnaire() {
        return idQuestionnaire;
    }

    public void setIdQuestionnaire(int idQuestionnaire) {
        this.idQuestionnaire = idQuestionnaire;
    }

    public int getIdPatient() {
        return idPatient;
    }

    public void setIdPatient(int idPatient) {
        this.idPatient = idPatient;
    }

    public int getIdDoctor() {
        return idDoctor;
    }

    public void setIdDoctor(int idDoctor) {
        this.idDoctor = idDoctor;
    }

    public String getDateOfCompletion() {
        return dateOfCompletion;
    }

    public void setDateOfCompletion(String dateOfCompletion) {
        this.dateOfCompletion = dateOfCompletion;
    }

    public String getFioPatient() {
        return fioPatient;
    }

    public void setFioPatient(String fioPatient) {
        this.fioPatient = fioPatient;
    }

    public String getDiagnosis() {
        return codeToDiagnosis(diagnosis);
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getFioDoctor() {
        return fioDoctor;
    }

    public void setFioDoctor(String fioDoctor) {
        this.fioDoctor = fioDoctor;
    }
}
