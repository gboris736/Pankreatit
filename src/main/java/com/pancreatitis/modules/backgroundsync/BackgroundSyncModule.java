package com.pancreatitis.modules.backgroundsync;

import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackgroundSyncModule {
    private static BackgroundSyncModule instance;
    private ScheduledExecutorService scheduler;
    private final long checkIntervalMinutes = 2;

    // Паттерн для извлечения даты из имени файла (после логина, перед .zip.enc)
    private static final Pattern DATE_PATTERN = Pattern.compile("_(\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2})\\.(?:zip\\.enc|req)");

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
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
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
                String newDate = extractDateFromArchiveFileName(fileName);

                // Если локальный файл существует – сравниваем даты
                if (pref_filename != null) {
                    String oldDate = extractDateFromArchiveFileName(pref_filename);
                    // Если дата нового файла меньше (раньше), удаляем его из облака и пропускаем
                    if (oldDate != null && newDate != null && newDate.compareTo(oldDate) < 0) {
                        cloud.deleteFile("/fetch/upload/" + fileName);
                        continue;
                    }
                }

                // Скачиваем новый файл
                byte[] data = cloud.downloadFile("/fetch/upload/" + fileName);
                if (data == null || data.length == 0) continue;

                // Сохраняем локально
                boolean saved = localStorage.saveArchive(login, fileName, data);
                if (saved) {
                    cloud.deleteFile("/fetch/upload/" + fileName);
                    // Удаляем старый локальный файл, если он был
                    if (pref_filename != null) {
                        localStorage.deleteArchive(login, pref_filename);
                    }
                }
            }
        } catch (Exception e) {

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
                    continue;
                }

                boolean uploaded = cloud.uploadFileToDownload(filename, archive);
                if (uploaded) {
                    cloud.deleteFile("/fetch/requests/" + reqFile);
                }
            }
        } catch (Exception e) {

        }
    }

    private String extractLoginFromUploadFileName(String fileName) {
        if (fileName == null || !fileName.startsWith("archive_")) {
            return null;
        }

        Matcher matcher = DATE_PATTERN.matcher(fileName);
        if (matcher.find()) {
            int suffixStart = matcher.start();
            return fileName.substring(8, suffixStart);
        }
        return null;
    }

    private String extractLoginFromRequestFileName(String fileName) {
        Matcher matcher = DATE_PATTERN.matcher(fileName);
        if (matcher.find()) {
            int suffixStart = matcher.start();
            return fileName.substring(0, suffixStart);
        }
        return null;
    }

    private String extractDateFromArchiveFileName(String fileName) {
        if (fileName == null) return null;
        Matcher matcher = DATE_PATTERN.matcher(fileName);
        return matcher.find() ? matcher.group(1) : null;
    }
}