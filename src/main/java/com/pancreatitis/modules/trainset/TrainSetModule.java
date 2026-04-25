package com.pancreatitis.modules.trainset;

import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TrainSetModule {
    private static LocalStorageModule localStorageModule;
    private static CloudStorageModule cloudStorageModule;
    private static TrainSetModule instance;
    private TrainingData trainingData;
    private String currentFileName;

    private TrainSetModule() {
        localStorageModule = LocalStorageModule.getInstance();
        cloudStorageModule = CloudStorageModule.getInstance();
    }

    public static TrainSetModule getInstance() {
        if (instance == null) {
            synchronized (TrainSetModule.class) {
                if (instance == null) {
                    instance = new TrainSetModule();
                }
            }
        }
        return instance;
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public String getLatestAlgorithmFileName() {
        List<String> files = localStorageModule.listAlgorithmFiles();
        if (files.isEmpty()) return null;

        DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("'algorithm_'yyyy_MM_dd_HH_mm_ss'.txt'");
        return files.stream()
                .max(Comparator.comparing(fname -> {
                    try {
                        return LocalDateTime.parse(fname, fileFormatter);
                    } catch (Exception e) {
                        return LocalDateTime.MIN; // некорректное имя – в конец
                    }
                }))
                .orElse(null);
    }

    public boolean saveOverwrite() {
        try {
            String prevFileName = currentFileName;
            boolean written = saveChanges();
            boolean del = false;
            if (written) {
                del = localStorageModule.deleteAlgorithmFile(prevFileName);
            }
            return del;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadFromFile(String fileName) {
        try {
            currentFileName = fileName;
            byte[] raw = localStorageModule.readAlgorithmFile(fileName);
            TrainingData data = TrainingDataParser.parseFromFile(new ByteArrayInputStream(raw));
            this.trainingData = data;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean saveChanges() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"));
            String newFileName = "algorithm_" + timestamp + ".txt";
            String content = TrainingDataParser.serializeToTextFormat(trainingData);
            boolean written = localStorageModule.writeAlgorithmFile(newFileName,
                    content.getBytes(StandardCharsets.UTF_8));
            if (written) {
                currentFileName = newFileName;
            }
            return written;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Integer> getQuestionIdTrainList() throws TrainingDataException{
        List<Integer> ret = new ArrayList<>();
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        for ( float[] record : trainingData.getTrainingRecords() ){
            int id = (int) record[0];
            if (databaseModule.containsQuestion(id)) {
                ret.add( id );
            }
            else {
                throw new TrainingDataException( String.format("Not found this questionnaire in data base, %d", id) );
            }
        }

        return ret;
    }

    public boolean submit() {
        try {
            String datetime = currentFileName.startsWith("algorithm_") && currentFileName.endsWith(".txt")
                    ? currentFileName.substring(10, currentFileName.length()-4)
                    : currentFileName.replaceAll("\\D+","");


            boolean dataUploaded = cloudStorageModule.uploadTrainingData(trainingData, datetime);
            if (!dataUploaded) {
                return false;
            }

            byte[] signature = signTrainingData(trainingData);
            boolean sigUploaded = cloudStorageModule.uploadTrainingDataSig(signature, datetime);
            if (!sigUploaded) {
                return false;
            }

            // Удаляем все старые файлы алгоритма, кроме только что загруженных
            cloudStorageModule.cleanOldAlgorithmFiles(datetime);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] signTrainingData(TrainingData data) throws Exception {
        PrivateKey privateKey = localStorageModule.loadEd25519PrivateKey();

        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(privateKey);

        // Преобразуем данные в текстовый формат и подписываем именно его
        String textData = TrainingDataParser.serializeToTextFormat(data);
        sig.update(textData.getBytes(StandardCharsets.UTF_8));

        return sig.sign();
    }

    public boolean addQuestionnaire(Questionnaire questionnaire, List<CharacterizationAnketPatient> characterizationAnketPatientLists) {
        try {
            int codeDiagnosis = Integer.parseInt(questionnaire.getDiagnosis());
            long idQuestionnaire = questionnaire.getId();
            float[] records = new float[characterizationAnketPatientLists.size() + 2];
            records[0] = idQuestionnaire;
            for(int i = 0; i < characterizationAnketPatientLists.size(); i++) {
                CharacterizationAnketPatient characterizationAnketPatient = characterizationAnketPatientLists.get(i);
                if (characterizationAnketPatient.getValue() != -1) {
                    records[i+1] = characterizationAnketPatient.getValue();
                } else {
                    records[i+1] = characterizationAnketPatient.getIdValue();
                }
            }
            records[characterizationAnketPatientLists.size() + 1] = codeDiagnosis;
            return trainingData.addRecord(records, codeDiagnosis);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteQuestionnaire(Questionnaire questionnaire) {
        try {
            long idQuestionnaire = questionnaire.getId();
            return trainingData.deleteRecord(idQuestionnaire);
        } catch (Exception e) {
            return false;
        }
    }
}
