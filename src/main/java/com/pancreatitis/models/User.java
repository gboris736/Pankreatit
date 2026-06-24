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
