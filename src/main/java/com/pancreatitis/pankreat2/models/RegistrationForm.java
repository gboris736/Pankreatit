package com.pancreatitis.pankreat2.models;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RegistrationForm {
    private String login;
    private String password;
    private String fullName;
    private String email;
    private String number;

    public RegistrationForm() {}
    public RegistrationForm(String login, String password, String fullName, String email, String number) {
        this.login = login;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.number = number;
    }

    public String getLogin() {
        return login;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static RegistrationForm fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, RegistrationForm.class);
    }
}
