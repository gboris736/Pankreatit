package com.pancreatitis.models;


import java.util.List;

public class CharacteristicItem {
    private int idCharacteristic;
    private int IdType;
    private String name;
    private String hint;
    private String createdAt = "-";
    private boolean isNewItem = false;

    public boolean isNewItem() {
        return isNewItem;
    }

    public void setNewItem(boolean newItem) {
        isNewItem = newItem;
    }

    public CharacteristicItem() {
    }

    public int getIdType() {
        return IdType;
    }

    public void setIdType(int idType) {
        IdType = idType;
    }

    public int getIdCharacteristic() {
        return idCharacteristic;
    }

    public void setIdCharacteristic(int idCharacteristic) {
        this.idCharacteristic = idCharacteristic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
