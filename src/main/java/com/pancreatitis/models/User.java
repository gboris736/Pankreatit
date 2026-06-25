package com.pancreatitis.models;

import javax.crypto.SecretKey;

public class User extends Doctor {
    private SecretKey key;
    private static User instance;
    public static User getInstance() {
        if (instance == null){
            instance = new User();
        }
        return instance;
    }

    public User() {
        super();
    }

    public Doctor getDoctor() {
        Doctor doctor = new Doctor();
        doctor.setEmail(User.getInstance().getEmail());
        doctor.setPhone(User.getInstance().getPhone());
        doctor.setLogin(User.getInstance().getLogin());
        doctor.setCreatedAt(User.getInstance().getCreatedAt());
        doctor.setId(User.getInstance().getId());
        doctor.setStatus(User.getInstance().getStatus());
        doctor.setFio(User.getInstance().getFio());
        return doctor;
    }

    public static void resetInstance() {
        instance = null;
    }

    public SecretKey getKey() {
        return key;
    }

    public void setKey(SecretKey key) {
        this.key = key;
    }
}
