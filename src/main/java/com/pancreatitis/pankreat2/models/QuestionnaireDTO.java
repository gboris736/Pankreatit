package com.pancreatitis.pankreat2.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class QuestionnaireDTO extends Questionnaire {
    private List<CharasteristicDTO> characteristicValues;

    public QuestionnaireDTO() {
        super();
    }
    public QuestionnaireDTO(Questionnaire questionnaire) {
        super(questionnaire);
    }

    public List<CharasteristicDTO> getCharacteristicValues() {
        return characteristicValues;
    }

    public void setCharacteristicValues(List<CharasteristicDTO> characteristicValues) {
        this.characteristicValues = characteristicValues;
    }
    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static QuestionnaireDTO fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, QuestionnaireDTO.class);
    }
}
