package com.pancreatitis.models;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class QuestionnaireItem {
    private long idQuestionnaire;
    private int idPatient;
    private String fioPatient;
    private String diagnosis = "-";
    private LocalDate data;
    public QuestionnaireItem() {}

    private String codeToDiagnosis(String code){
        Map<String, String> diagnosisMap = new HashMap<>();
        diagnosisMap.put("1", "Отечный панкреатит");
        diagnosisMap.put("5", "Панкреонекроз среднетяжелое течение");
        diagnosisMap.put("6", "Панкреонекроз тяжелое течение");
        return diagnosisMap.getOrDefault(code, "-");
    }

    public long getIdQuestionnaire() {
        return idQuestionnaire;
    }

    public void setIdQuestionnaire(long idQuestionnaire) {
        this.idQuestionnaire = idQuestionnaire;
    }

    public int getIdPatient() {
        return idPatient;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public void setIdPatient(int idPatient) {
        this.idPatient = idPatient;
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
}
