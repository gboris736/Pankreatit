package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class Update {
    private List<Patient> patientList;
    private List<QuestionnaireDTO> questionnaireDTOS;

    public List<Patient> getPatientList() {
        return patientList;
    }

    public void setPatientList(List<Patient> patientList) {
        this.patientList = patientList;
    }

    public List<QuestionnaireDTO> getQuestionnaireDTOS() {
        return questionnaireDTOS;
    }

    public void setQuestionnaireDTOS(List<QuestionnaireDTO> questionnaireDTOS) {
        this.questionnaireDTOS = questionnaireDTOS;
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
