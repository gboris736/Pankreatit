package com.pancreatitis.modules.updates;

import com.pancreatitis.models.*;
import java.util.List;

public class UpdateLoadResult {
    private final int index;
    private final Patient patient;
    private final Questionnaire questionnaire;
    private final List<CharacterizationAnketPatient> characteristics;
    private final boolean success;
    private final String errorMessage;
    private final String fileName;

    public UpdateLoadResult(int index, Patient patient, Questionnaire questionnaire,
                            List<CharacterizationAnketPatient> characteristics,
                            boolean success, String errorMessage, String fileName) {
        this.index = index;
        this.patient = patient;
        this.questionnaire = questionnaire;
        this.characteristics = characteristics;
        this.success = success;
        this.errorMessage = errorMessage;
        this.fileName = fileName;
    }

    // Геттеры
    public int getIndex() { return index; }
    public Patient getPatient() { return patient; }
    public Questionnaire getQuestionnaire() { return questionnaire; }
    public List<CharacterizationAnketPatient> getCharacteristics() { return characteristics; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public String getFileName() { return fileName; }
}