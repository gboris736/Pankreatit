package com.pancreatitis.modules.backgroundsync;

import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackgroundSyncModule {
    private static final Logger LOGGER = Logger.getLogger(BackgroundSyncModule.class.getName());
    private static BackgroundSyncModule instance;
    private ScheduledExecutorService scheduler;
    private final long checkIntervalMinutes = 30;

    private BackgroundSyncModule() {}

    public static BackgroundSyncModule getInstance() {
        if (instance == null) instance = new BackgroundSyncModule();
        return instance;
    }

    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkUploadFolder, 0, checkIntervalMinutes, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::checkRequestFolder, 0, checkIntervalMinutes, TimeUnit.MINUTES);
        LOGGER.info("BackgroundSyncModule started");
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            try { if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) LOGGER.warning("Scheduler shutdown timeout"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            scheduler = null;
        }
    }

    private void checkUploadFolder() {
        try {
            CloudStorageModule cloud = CloudStorageModule.getInstance();
            List<String> files = cloud.listUploadFiles();
            if (files.isEmpty()) return;

            LocalStorageModule localStorage = LocalStorageModule.getInstance();
            for (String fileName : files) {
                String login = extractLoginFromUploadFileName(fileName);
                if (login == null) continue;

                String pref_filename = localStorage.findArchiveFileNameInUserDir(login);
                if (pref_filename == null) continue;

                byte[] data = cloud.downloadFile("/fetch/upload/" + fileName);
                if (data == null || data.length == 0) continue;

                boolean saved = localStorage.saveArchive(login, fileName, data);
                if (saved) {
                    cloud.deleteFile("/fetch/upload/" + fileName);
                    localStorage.deleteArchive(login, pref_filename);
                    LOGGER.info("Downloaded and saved archive for " + login);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "checkUploadFolder error", e);
        }
    }

    private void checkRequestFolder() {
        try {
            CloudStorageModule cloud = CloudStorageModule.getInstance();
            List<String> reqFiles = cloud.listRequestFiles();
            if (reqFiles.isEmpty()) return;

            LocalStorageModule localStorage = LocalStorageModule.getInstance();
            for (String reqFile : reqFiles) {
                String login = extractLoginFromRequestFileName(reqFile);
                if (login == null) continue;

                String filename = localStorage.findArchiveFileNameInUserDir(login);
                if (filename == null) continue;

                byte[] archive = localStorage.readArchive(login, filename);
                if (archive == null) {
                    LOGGER.warning("No local archive for " + login);
                    continue;
                }

                boolean uploaded = cloud.uploadFileToDownload(filename, archive);
                if (uploaded) {
                    cloud.deleteFile("/fetch/requests/" + reqFile);
                    LOGGER.info("Uploaded archive for " + login + " to fetch/download");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "checkRequestFolder error", e);
        }
    }

    private String extractLoginFromUploadFileName(String fileName) {
        if (fileName.startsWith("archive_")) {
            String withoutPrefix = fileName.substring(8);
            int lastUnderscore = withoutPrefix.lastIndexOf('_');
            if (lastUnderscore > 0) return withoutPrefix.substring(0, lastUnderscore);
        }
        return null;
    }

    private String extractLoginFromRequestFileName(String fileName) {
        int idx = fileName.indexOf('_');
        return idx > 0 ? fileName.substring(0, idx) : null;
    }
}