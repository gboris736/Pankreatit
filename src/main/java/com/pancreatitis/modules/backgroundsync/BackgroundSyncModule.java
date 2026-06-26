package com.pancreatitis.modules.backgroundsync;

import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackgroundSyncModule {
    private static BackgroundSyncModule instance;
    private ScheduledExecutorService scheduler;
    private final long checkIntervalMinutes = 2;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private Thread shutdownHook;

    private static final Pattern DATE_PATTERN = Pattern.compile("_(\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2})\\.(?:zip\\.enc|req)");

    private BackgroundSyncModule() {}

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
        if (isRunning.get() || isShuttingDown.get()) {
            return;
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            stop();
        }

        try {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("BackgroundSync-Scheduler");
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });

            isRunning.set(true);

            scheduler.scheduleAtFixedRate(this::safeCheckUploadFolder, 0, checkIntervalMinutes, TimeUnit.MINUTES);
            scheduler.scheduleAtFixedRate(this::safeCheckRequestFolder, 0, checkIntervalMinutes, TimeUnit.MINUTES);

            if (shutdownHook == null) {
                shutdownHook = new Thread(() -> {
                    isShuttingDown.set(true);
                    isRunning.set(false);
                    if (scheduler != null) {
                        try {
                            scheduler.shutdownNow();
                        } catch (Exception e) {
                            // ignore
                        } finally {
                            scheduler = null;
                        }
                    }
                });
                try {
                    Runtime.getRuntime().addShutdownHook(shutdownHook);
                } catch (IllegalStateException e) {
                    // ignore
                }
            }

        } catch (Exception e) {
            isRunning.set(false);
            if (scheduler != null) {
                try {
                    scheduler.shutdownNow();
                } catch (Exception ex) {
                    // ignore
                } finally {
                    scheduler = null;
                }
            }
        }
    }

    public void stop() {
        if (!isRunning.get()) {
            return;
        }

        isShuttingDown.set(true);
        isRunning.set(false);

        if (scheduler != null) {
            try {
                scheduler.shutdownNow();
            } catch (Exception e) {
                // ignore
            } finally {
                scheduler = null;
            }
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            System.gc();
        } catch (Exception e) {
            // ignore
        }
    }

    public boolean isRunning() {
        return isRunning.get() && scheduler != null && !scheduler.isShutdown();
    }

    private void safeCheckUploadFolder() {
        if (!isRunning.get() || isShuttingDown.get()) {
            return;
        }

        try {
            checkUploadFolder();
        } catch (Exception e) {
            // ignore
        }
    }

    private void safeCheckRequestFolder() {
        if (!isRunning.get() || isShuttingDown.get()) {
            return;
        }

        try {
            checkRequestFolder();
        } catch (Exception e) {
            // ignore
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

            for (String fileName : files) {
                if (!isRunning.get() || isShuttingDown.get()) {
                    break;
                }

                String login = extractLoginFromUploadFileName(fileName);
                if (login == null) {
                    continue;
                }

                String pref_filename = localStorage.findArchiveFileNameInUserDir(login);
                String newDate = extractDateFromArchiveFileName(fileName);

                if (pref_filename != null) {
                    String oldDate = extractDateFromArchiveFileName(pref_filename);
                    if (oldDate != null && newDate != null && newDate.compareTo(oldDate) < 0) {
                        cloud.deleteFile("/fetch/upload/" + fileName);
                        continue;
                    }
                }

                byte[] data = cloud.downloadFile("/fetch/upload/" + fileName);

                if (data == null || data.length == 0) {
                    continue;
                }

                boolean saved = localStorage.saveArchive(login, fileName, data);
                if (saved) {
                    cloud.deleteFile("/fetch/upload/" + fileName);
                    if (pref_filename != null) {
                        localStorage.deleteArchive(login, pref_filename);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
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

            for (String reqFile : reqFiles) {
                if (!isRunning.get() || isShuttingDown.get()) {
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
                }
            }
        } catch (Exception e) {
            // ignore
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