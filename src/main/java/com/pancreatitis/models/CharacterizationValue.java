package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CharacterizationValue {
    private long id = -1;
    private int idCharacteristic = -1;
    private int idValue = -1;
    private String value;
    private String lastModified;

    public CharacterizationValue() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getIdCharacteristic() { return idCharacteristic; }
    public void setIdCharacteristic(int idCharacteristic) { this.idCharacteristic = idCharacteristic; }

    public int getIdValue() { return idValue; }
    public void setIdValue(int idValue) { this.idValue = idValue; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static CharacterizationValue fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, CharacterizationValue.class);
    }
}