package com.pancreatitis.modules.localstorage;

import java.nio.file.Path;
import java.util.HashMap;

public class DiskStorageControl {
    private final static String appName = "PankreatManager";
    private final static String appDB = "dataBaseStor";
    private final static String appAlg = "appAlgStor";
    private final static String dbFileName = "pancreatitis_v4.db";
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
            pathLibrary.put("aapDir", appDir);

            Path appDBDir = CrossPlatformStorage.getApplicationDataDirectory(appName, appDB);
            pathLibrary.put("aapDBDir", appDBDir);
            Path appDBPath = appDBDir.resolve(dbFileName);
            pathLibrary.put("aapDBPath", appDBPath);

            Path appAlgDir = CrossPlatformStorage.getApplicationDataDirectory(appName, appAlg);
            pathLibrary.put("aapAldDir", appAlgDir);
            Path appAlgPath = appAlgDir.resolve(algFileName);
            pathLibrary.put("aapAlgPath", appAlgPath);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    public HashMap<String, Path> getPathLibrary() {
        return new HashMap<>(pathLibrary);
    }
}
