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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Сохраняет выборку с тем же пользовательским именем (если оно было),
     * но с новой временной меткой. Если имя файла было без пользовательского
     * имени (только algorithm_timestamp.txt), то сохраняется новое имя без
     * пользовательской части.
     */
    public boolean saveOverwrite() {
        if (currentFileName == null) {
            // Если текущий файл не задан – создаём новый, как при первом сохранении
            return saveNewFile("experiment"); // или можно выбросить ошибку
        }

        // 1. Разбираем текущее имя
        //    Ищем timestamp: всегда последние 6 групп цифр, разделённых '_'.
        Pattern pattern = Pattern.compile(
                "algorithm_([^_]+_)?(\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2})\\.txt$");
        Matcher matcher = pattern.matcher(currentFileName);

        String namePrefix = "";   // пользовательская часть с завершающим '_' или пусто
        if (matcher.find()) {
            String userPart = matcher.group(1);  // например "my_exp_" или null
            if (userPart != null && !userPart.isEmpty()) {
                namePrefix = userPart;           // уже с '_'
            }
        } else {
            // имя не соответствует шаблону – fallback
            return saveNewFile("updated");
        }

        // 2. Генерируем новый timestamp
        String newTimestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"));

        // 3. Формируем новое имя файла
        String newFileName = "algorithm_" + namePrefix + newTimestamp + ".txt";

        // 4. Сериализуем и записываем
        String content = TrainingDataParser.serializeToTextFormat(trainingData);

        try {
            boolean ok = localStorageModule.writeAlgorithmFile(newFileName,
                    content.getBytes(StandardCharsets.UTF_8));
            if (ok) {
                this.currentFileName = newFileName;   // переключаемся на новый файл
            }
            return ok;
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

    public boolean saveNewFile(String name) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"));
        String filename = "algorithm_" + name + "_" + timestamp + ".txt";

        String content = TrainingDataParser.serializeToTextFormat(trainingData);

        try {
            localStorageModule.writeAlgorithmFile(filename, content.getBytes(StandardCharsets.UTF_8));
            this.currentFileName = filename;
            return true;
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
            String currentFile = getCurrentFileName();
            // Регулярка: захватывает timestamp независимо от наличия пользовательского имени
            Pattern pattern = Pattern.compile(
                    "algorithm_([^_]+_)?(\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2})\\.txt$");
            Matcher matcher = pattern.matcher(currentFile);
            String timestamp;
            if (matcher.find()) {
                timestamp = matcher.group(2);   // группа с датой/временем
            } else {
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"));
            }


            boolean dataUploaded = cloudStorageModule.uploadTrainingData(trainingData, timestamp);
            if (!dataUploaded) {
                return false;
            }

            byte[] signature = signTrainingData(trainingData);
            boolean sigUploaded = cloudStorageModule.uploadTrainingDataSig(signature, timestamp);
            if (!sigUploaded) {
                return false;
            }

            // Удаляем все старые файлы алгоритма, кроме только что загруженных
            cloudStorageModule.cleanOldAlgorithmFiles(timestamp);

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
