package com.pancreatitis.modules.updates;

import com.pancreatitis.models.*;
import com.pancreatitis.modules.authorization.AuthorizationModule;
import com.pancreatitis.modules.safety.SafetyModule;
import javafx.util.Pair;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.concurrent.Callable;

public class UpdateProcessingTask implements Callable<UpdateLoadResult> {
    private final int index;
    private final Pair<Pair<String, String>, Update> updatePair;

    public UpdateProcessingTask(int index, Pair<Pair<String, String>, Update> updatePair) {
        this.index = index;
        this.updatePair = updatePair;
    }

    @Override
    public UpdateLoadResult call() throws Exception {
        try {
            Update update = updatePair.getValue();
            String fileName = updatePair.getKey().getValue();

            // 1. Обработка пациента
            Patient patient = update.getPatient();
            patient.setId(-1);

            // 2. Обработка анкеты
            QuestionnaireDTO dto = update.getQuestionnaireDTO();
            Questionnaire questionnaire = new Questionnaire(dto);
            questionnaire.setId(-1);

            // 3. Получение характеристик
            List<CharacterizationAnketPatient> characteristics = dto.getCharacteristicValues();

            return new UpdateLoadResult(index, patient, questionnaire, characteristics,
                    true, null, fileName);

        } catch (Exception ex) {
            return new UpdateLoadResult(index, null, null, null, false,
                    ex.getMessage(), null);
        }
    }
}