package com.pancreatitis.pankreat2.modules.prediction;

import android.content.Context;

import com.pancreatitis.pankreat2.models.Characteristic;
import com.pancreatitis.pankreat2.models.CharasteristicDTO;
import com.pancreatitis.pankreat2.modules.localstorage.LocalStorageModule;
import com.pancreatitis.pankreat2.modules.trainset.TrainingData;
import com.pancreatitis.pankreat2.modules.trainset.TrainingDataParser;

import java.io.ByteArrayInputStream;
import java.util.*;

public class PredictionModule {
    private SimilarityBasedModel currentModel;
    private List<Characteristic> lastLoadedCharacteristics;
    private static PredictionModule instance;
    private Context context;
    private byte[] cachedTrainingData;

    private PredictionModule() {
        this.currentModel = null;
        this.lastLoadedCharacteristics = null;
        this.cachedTrainingData = null;
    }

    public static PredictionModule getInstance(){
        if (instance == null){
            instance = new PredictionModule();
        }
        return instance;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public PredictionResult predict(List<CharasteristicDTO> characteristicDTOs) throws Exception {
        if (context == null) {
            throw new IllegalStateException("Context not set. Call setContext() first.");
        }
        LocalStorageModule localStorageModule = LocalStorageModule.getInstance();
        localStorageModule.setContext(getContext());

        // 1. Получаем тренировочные данные из assets
        byte[] rawTrainingData = localStorageModule.getTrainingData();

        TrainingData trainingData = TrainingDataParser.parseFromFile(
                new ByteArrayInputStream(rawTrainingData)
        );

        // 2. Преобразуем DTO в Characteristic (без значения)
        List<Characteristic> characteristics = extractCharacteristicsFromDTO(characteristicDTOs);
        this.lastLoadedCharacteristics = characteristics;

        // 3. Создаем модель
        currentModel = new SimilarityBasedModel(trainingData, characteristics);

        // 4. Извлекаем признаки из DTO
        float[] features = extractFeaturesFromDTO(characteristicDTOs, trainingData);

        // 5. Выполняем прогнозирование
        return currentModel.predict(features);
    }

    /**
     * Извлекает список Characteristic из DTO (без значений)
     */
    private List<Characteristic> extractCharacteristicsFromDTO(List<CharasteristicDTO> dtoList) {
        List<Characteristic> characteristics = new ArrayList<>();

        for (CharasteristicDTO dto : dtoList) {
            Characteristic ch = new Characteristic();
            ch.setId(dto.getId());
            ch.setName(dto.getName());
            ch.setOpis(dto.getOpis());
            ch.setIdType(dto.getIdType());
            ch.setWeight(dto.getWeight());
            ch.setMinValue(dto.getMinValue());
            ch.setMaxValue(dto.getMaxValue());
            characteristics.add(ch);
        }

        return characteristics;
    }

    /**
     * Извлекает признаки с учетом пересечения характеристик по id
     */
    private float[] extractFeaturesFromDTO(List<CharasteristicDTO> dtoList,
                                           TrainingData trainingData) {
        // Получаем id характеристик из тренировочных данных
        int[] trainingCharIds = trainingData.getCharacteristicIds();

        // Создаем мапу DTO по id для быстрого поиска
        Map<Integer, CharasteristicDTO> dtoMap = new HashMap<>();
        for (CharasteristicDTO dto : dtoList) {
            dtoMap.put(dto.getId(), dto);
        }

        // Создаем массив признаков по размеру тренировочных характеристик
        float[] features = new float[trainingCharIds.length];

        for (int i = 0; i < trainingCharIds.length; i++) {
            int charId = trainingCharIds[i];

            // Проверяем, есть ли эта характеристика в DTO
            CharasteristicDTO dto = dtoMap.get(charId);
            if (dto != null) {
                // Характеристика есть в DTO - используем ее значение
                features[i] = dto.getValue();
            } else {
                // Характеристики нет в DTO - используем дефолтное значение (0)
                features[i] = 0f;
            }
        }

        return features;
    }

    public boolean isReady() {
        return currentModel != null;
    }

    public List<Characteristic> getLastLoadedCharacteristics() {
        return lastLoadedCharacteristics != null
                ? new ArrayList<>(lastLoadedCharacteristics)
                : Collections.emptyList();
    }
}