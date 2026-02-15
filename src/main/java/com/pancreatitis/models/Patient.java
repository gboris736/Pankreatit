package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Patient {
    private long id = -1;
    private String fio;
    private String lastModified;

    // Конструкторы
    public Patient() {}

    public Patient(Integer id, String fio, String lastModified) {
        this.id = id;
        this.fio = fio;
        this.lastModified = lastModified;
    }

    // Геттеры и сеттеры
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFio() { return fio; }
    public void setFio(String fio) { this.fio = fio; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static Patient fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Patient.class);
    }
}