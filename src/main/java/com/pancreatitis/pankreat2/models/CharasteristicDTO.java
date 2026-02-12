// CharasteristicDTO.java
package com.pancreatitis.pankreat2.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CharasteristicDTO extends Characteristic {
    private float value;

    public CharasteristicDTO() {
        super();
    }

    public CharasteristicDTO(Characteristic characteristic) {
        super(characteristic);
    }
    public CharasteristicDTO(Characteristic characteristic, float value) {
        super(characteristic);
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public int getIntValue() {
        return Math.round(value);
    }
    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static CharasteristicDTO fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, CharasteristicDTO.class);
    }
}