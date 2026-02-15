package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class Characteristic {
    private int id;
    private String name;
    private String opis;
    private int idType;
    private String hints;
    private float weight;
    private float minValue;
    private float maxValue;
    private String lastModified;

    // Конструкторы
    public Characteristic() {}

    public Characteristic(Characteristic characteristic) {
        this.id = characteristic.getId();
        this.name = characteristic.getName();
        this.opis = characteristic.getOpis();
        this.idType = characteristic.getIdType();
        this.hints = characteristic.getHints();
        this.weight = characteristic.getWeight();
        this.minValue = characteristic.getMinValue();
        this.maxValue = characteristic.getMaxValue();
        this.lastModified = characteristic.getLastModified();
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOpis() { return opis; }
    public void setOpis(String opis) { this.opis = opis; }

    public int getIdType() { return idType; }
    public void setIdType(int type) { this.idType = type; }

    public String getHints() { return hints; }
    public void setHints(String hints) { this.hints = hints; }

    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = weight; }

    public float getMinValue() { return minValue; }
    public void setMinValue(float minValue) { this.minValue = minValue; }

    public float getMaxValue() { return maxValue; }
    public void setMaxValue(float maxValue) { this.maxValue = maxValue; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static Characteristic fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Characteristic.class);
    }
}