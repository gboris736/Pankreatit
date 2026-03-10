package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Update {
    private Patient patient;
    private QuestionnaireDTO questionnaireDTO;

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public QuestionnaireDTO getQuestionnaireDTO() {
        return questionnaireDTO;
    }

    public void setQuestionnaireDTO(QuestionnaireDTO questionnaireDTO) {
        this.questionnaireDTO = questionnaireDTO;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static Update fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Update.class);
    }
}