package com.pancreatitis.pankreat2.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class Questionnaire {
    private long id = -1;
    private long idPatient = -1;
    private int idDoctor = -1;
    private int idExpert = -1;
    private String diagnosis;
    private String dateOfCompletion;
    private String admittedFrom;
    private String lastModified;

    // Конструкторы
    public Questionnaire() {}

    public Questionnaire(Questionnaire questionnaire) {
        this.id = questionnaire.getId();
        this.idPatient = questionnaire.getIdPatient();
        this.idDoctor = questionnaire.getIdDoctor();
        this.idExpert = questionnaire.getIdExpert();
        this.diagnosis = questionnaire.getCodeDiagnosis();
        this.dateOfCompletion = questionnaire.getDateOfCompletion();
        this.admittedFrom = questionnaire.getAdmittedFrom();
        this.lastModified = questionnaire.getLastModified();
    }
    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static Questionnaire fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Questionnaire.class);
    }

    @Override
    public String toString() {
        return "{" +
                "diagnosis='" + diagnosis + '\'' +
                ", admittedFrom='" + admittedFrom + '\'' +
                ", lastModified=" + lastModified +
                '}';
    }

    private String codeToDiagnosis(String code){
        Map<String, String> diagnosisMap = new HashMap<>();
        diagnosisMap.put("1", "Отечный панкреатит");
        diagnosisMap.put("5", "Панкреонекроз среднетяжелое течение");
        diagnosisMap.put("6", "Панкреонекроз тяжелое течение");
        return diagnosisMap.getOrDefault(code, "-");
    }

    // Геттеры и сеттеры
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getIdPatient() {
        return idPatient;
    }

    public void setIdPatient(long idPatient) {
        this.idPatient = idPatient;
    }

    public int getIdDoctor() {
        return idDoctor;
    }

    public void setIdDoctor(int idDoctor) {
        this.idDoctor = idDoctor;
    }

    public void setIdExpert(int idExpert) {
        this.idExpert = idExpert;
    }

    public Integer getIdExpert() { return idExpert; }
    public void setIdExpert(Integer idExpert) { this.idExpert = idExpert; }

    public String getDiagnosis() { return codeToDiagnosis(diagnosis); }
    public String getCodeDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getDateOfCompletion() { return dateOfCompletion; }
    public void setDateOfCompletion(String dateOfCompletion) { this.dateOfCompletion = dateOfCompletion; }

    public String getAdmittedFrom() { return admittedFrom; }
    public void setAdmittedFrom(String admittedFrom) { this.admittedFrom = admittedFrom; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

}