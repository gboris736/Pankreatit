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
    private volatile boolean isRunning = false;

    // Паттерн для извлечения даты из имени файла (после логина, перед .zip.enc)
    private static final Pattern DATE_PATTERN = Pattern.compile("_(\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2})\\.(?:zip\\.enc|req)");

    private BackgroundSyncModule() {
        // Добавляем Shutdown Hook для гарантированной очистки
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
        }));
    }

    public static BackgroundSyncModule getInstance() {
        if (instance == null) {
            synchronized (BackgroundSyncModule.class) {
                if (instance == null) {
                    instance = new BackgroundSyncModule();
                }
            }
        }
        return instance;
    }

    public void start() {
        if (isRunning) {
            return;
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            stop();
        }

        try {
            // Создаем планировщик с daemon-потоками для автоматического завершения при закрытии приложения
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);  // Критично для Windows: поток завершится при остановке JVM
                t.setName("BackgroundSync-Scheduler");
                t.setPriority(Thread.MIN_PRIORITY); // Низкий приоритет, чтобы не мешать UI
                return t;
            });

            isRunning = true;

            // Запускаем задачи с фиксированной задержкой
            scheduler.scheduleAtFixedRate(this::safeCheckUploadFolder, 0, checkIntervalMinutes, TimeUnit.MINUTES);
            scheduler.scheduleAtFixedRate(this::safeCheckRequestFolder, 0, checkIntervalMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            isRunning = false;
            if (scheduler != null) {
                scheduler.shutdownNow();
                scheduler = null;
            }
        }
    }

    public void stop() {
        isRunning = false;

        if (scheduler == null) {
            return;
        }

        // Отключаем выполнение новых задач
        scheduler.shutdown();

        try {
            // Ждем завершения текущих задач с таймаутом
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            scheduler = null;
        }
    }

    public boolean isRunning() {
        return isRunning && scheduler != null && !scheduler.isShutdown();
    }

    public long getCheckIntervalMinutes() {
        return checkIntervalMinutes;
    }

    // Безопасная обертка для checkUploadFolder с логированием ошибок
    private void safeCheckUploadFolder() {
        if (!isRunning) {
            return;
        }

        try {
            checkUploadFolder();
        } catch (Exception e) {

        }
    }

    // Безопасная обертка для checkRequestFolder с логированием ошибок
    private void safeCheckRequestFolder() {
        if (!isRunning) {
            return;
        }

        try {
            checkRequestFolder();
        } catch (Exception e) {

        }
    }

    private void checkUploadFolder() {
        try {
            CloudStorageModule cloud = CloudStorageModule.getInstance();
            List<String> files = cloud.listUploadFiles();

            if (files.isEmpty()) {
                return;
            }

            LocalStorageModule localStorage = LocalStorageModule.getInstance();
            int processedCount = 0;

            for (String fileName : files) {
                if (!isRunning) {
                    break;
                }

                String login = extractLoginFromUploadFileName(fileName);
                if (login == null) {
                    continue;
                }

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

                byte[] data = cloud.downloadFile("/fetch/upload/" + fileName);

                if (data == null || data.length == 0) {
                    continue;
                }

                // Сохраняем локально
                boolean saved = localStorage.saveArchive(login, fileName, data);
                if (saved) {
                    cloud.deleteFile("/fetch/upload/" + fileName);

                    // Удаляем старый локальный файл, если он был
                    if (pref_filename != null) {
                        localStorage.deleteArchive(login, pref_filename);
                    }
                    processedCount++;
                }
            }
        } catch (Exception e) {

        }
    }

    private void checkRequestFolder() {
        try {
            CloudStorageModule cloud = CloudStorageModule.getInstance();
            List<String> reqFiles = cloud.listRequestFiles();

            if (reqFiles.isEmpty()) {
                return;
            }

            LocalStorageModule localStorage = LocalStorageModule.getInstance();
            int processedCount = 0;

            for (String reqFile : reqFiles) {
                if (!isRunning) {
                    break;
                }

                String login = extractLoginFromRequestFileName(reqFile);
                if (login == null) {
                    continue;
                }

                String filename = localStorage.findArchiveFileNameInUserDir(login);
                if (filename == null) {
                    continue;
                }

                byte[] archive = localStorage.readArchive(login, filename);
                if (archive == null) {
                    continue;
                }

                boolean uploaded = cloud.uploadFileToDownload(filename, archive);
                if (uploaded) {
                    cloud.deleteFile("/fetch/requests/" + reqFile);
                    processedCount++;
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
        if (fileName == null) {
            return null;
        }

        Matcher matcher = DATE_PATTERN.matcher(fileName);
        if (matcher.find()) {
            int suffixStart = matcher.start();
            return fileName.substring(0, suffixStart);
        }
        return null;
    }

    private String extractDateFromArchiveFileName(String fileName) {
        if (fileName == null) {
            return null;
        }

        Matcher matcher = DATE_PATTERN.matcher(fileName);
        return matcher.find() ? matcher.group(1) : null;
    }
}