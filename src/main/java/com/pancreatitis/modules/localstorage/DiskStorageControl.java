package com.pancreatitis.modules.localstorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DiskStorageControl {
    private final static String appName = "PankreatManager";
    private final static String appDB = "dataBaseStor";
    private final static String appAlg = "appAlgStor";
    private final static String dbFileName = "pancreatitis_v6.db";
    private final static String algFileName = "algorithm.txt";

    private HashMap<String, Path> pathLibrary;
    private static DiskStorageControl instance;

    public static synchronized DiskStorageControl getInstance(){
        if (instance == null){
            instance = new DiskStorageControl();
        }
        return instance;
    }
    private DiskStorageControl(){
        pathLibrary = new HashMap<>();
        try {
            Path appDir = CrossPlatformStorage.getApplicationDataDirectory(appName);
            pathLibrary.put("appDir", appDir);

            Path appDBDir = CrossPlatformStorage.getApplicationDataDirectory(appName, appDB);
            pathLibrary.put("appDBDir", appDBDir);
            Path appDBPath = appDBDir.resolve(dbFileName);
            pathLibrary.put("appDBPath", appDBPath);

            Path appAlgDir = CrossPlatformStorage.getApplicationDataDirectory(appName, appAlg);
            pathLibrary.put("appAlgDir", appAlgDir);
            Path appAlgPath = appAlgDir.resolve(algFileName);
            pathLibrary.put("appAlgPath", appAlgPath);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    public HashMap<String, Path> getListFilesRecursively() {
        return new HashMap<>(pathLibrary);
    }

    public Path getAppDir() {
        return pathLibrary.get("appDir");
    }

    public Path getDBPath() { return pathLibrary.get("appDBPath"); }
    public Path getAlgPath() { return pathLibrary.get("appAlgPath"); }

    public List<Path> getListFilesInPath(Path path) throws  IOException {

        return CrossPlatformStorage.listFilesRecursively(path);

    }
}
