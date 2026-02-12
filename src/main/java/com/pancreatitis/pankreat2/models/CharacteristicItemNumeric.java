package com.pancreatitis.pankreat2.models;

public class CharacteristicItemNumeric extends CharacteristicItem {
    private float value = -1F;
    public CharacteristicItemNumeric() {
        super();
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
