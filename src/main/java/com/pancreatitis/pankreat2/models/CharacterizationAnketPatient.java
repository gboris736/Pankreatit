package com.pancreatitis.pankreat2.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CharacterizationAnketPatient {
    private long idAnket = -1;
    private int idCharacteristic = -1;
    private int idValue = 0;
    private float value = -1;
    private String createdAt;
    private String lastModified;

    public CharacterizationAnketPatient() {}

    public long getIdAnket() { return idAnket; }
    public void setIdAnket(long idAnket) { this.idAnket = idAnket; }

    public int getIdValue() { return idValue; }
    public void setIdValue(int id_value) { this.idValue = id_value; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public int getIdCharacteristic() {
        return idCharacteristic;
    }

    public void setIdCharacteristic(int idCharacteristic) {
        this.idCharacteristic = idCharacteristic;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static CharacterizationAnketPatient fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, CharacterizationAnketPatient.class);
    }
}