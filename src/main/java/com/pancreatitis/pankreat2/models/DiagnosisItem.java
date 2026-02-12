package com.pancreatitis.pankreat2.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiagnosisItem {
    private final String name = "Диагноз";
    private final List<String> options = new ArrayList<>(Arrays.asList("Нет данных", "Отечный панкреатит", "Панкреонекроз среднетяжелое течение", "Панкреонекроз тяжелое течение"));

    private int value = 0;
    public DiagnosisItem() {
    }

    public String getName() {
        return name;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getValue() {
        return value;
    }
    public void setValue(int value) {
        this.value = value;
    }
    public String getCodeDiagnosis(){
        if (value == 0){
            return "";
        } else if (value == 1) {
            return "1";
        } else if (value == 2) {
            return "5";
        } else {
            return "6";
        }
    }
}
