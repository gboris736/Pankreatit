package com.pancreatitis.models;

import java.util.List;

public class CharacteristicItemNonNumeric extends CharacteristicItem {
    private List<String> options;
    private int value = 0;

    public CharacteristicItemNonNumeric() {
        super();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
