package com.pancreatitis.modules.diskStorControl;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.stream.Stream;

class CrossPlatformStorage {

    /**
     * Получает основную директорию данных приложения
     */
    public static Path getApplicationDataDirectory(String appName) throws IOException {
        return getApplicationDataDirectory(appName, (String[]) null);
    }

    /**
     * Получает директорию с поддиректориями
     * @param appName Имя приложения
     * @param subdirs Массив поддиректорий для создания (может быть null)
     */
    public static Path getApplicationDataDirectory(String appName, String... subdirs) throws IOException {
        Objects.requireNonNull(appName, "Application name cannot be null");

        // Определяем базовую директорию для приложения
        Path baseDir = determinePlatformSpecificDir(appName);

        // Формируем полный путь с поддиректориями
        Path targetDir = baseDir;
        if (subdirs != null && subdirs.length > 0) {
            for (String subdir : subdirs) {
                if (subdir != null && !subdir.trim().isEmpty()) {
                    targetDir = targetDir.resolve(subdir.trim());
                }
            }
        }

        // Создаем директорию(и) с правильными правами
        createDirectoryWithPermissions(targetDir);

        return targetDir;
    }

    /**
     * Создает директорию и все родительские директории с правильными правами
     */
    private static void createDirectoryWithPermissions(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            // Создаем все родительские директории с правильными правами
            createParentDirectoriesWithPermissions(dir.getParent());

            // Создаем целевую директорию
            Files.createDirectory(dir);
            setPlatformSpecificPermissions(dir);
        } else if (!Files.isDirectory(dir)) {
            throw new IOException("Path exists but is not a directory: " + dir);
        }
    }

    /**
     * Создает все родительские директории с правильными правами
     */
    private static void createParentDirectoriesWithPermissions(Path parentDir) throws IOException {
        if (parentDir == null || Files.exists(parentDir)) {
            return;
        }

        // Рекурсивно создаем родительские директории
        createParentDirectoriesWithPermissions(parentDir.getParent());

        if (!Files.exists(parentDir)) {
            Files.createDirectory(parentDir);
            setPlatformSpecificPermissions(parentDir);
        }
    }

    /**
     * Определяет базовую директорию в зависимости от ОС
     */
    private static Path determinePlatformSpecificDir(String appName) {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return getWindowsDataDir(appName);
        } else if (os.contains("mac")) {
            return getMacDataDir(appName);
        } else {
            return getLinuxDataDir(appName);
        }
    }

    private static Path getWindowsDataDir(String appName) {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isEmpty()) {
            return Paths.get(appData, appName);
        }
        return Paths.get(System.getProperty("user.home"), "AppData", "Roaming", appName);
    }

    private static Path getMacDataDir(String appName) {
        return Paths.get(System.getProperty("user.home"),
                "Library", "Application Support", appName);
    }

    private static Path getLinuxDataDir(String appName) {
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome != null && !xdgDataHome.isEmpty()) {
            return Paths.get(xdgDataHome, appName.toLowerCase());
        }
        return Paths.get(System.getProperty("user.home"),
                ".local", "share", appName.toLowerCase());
    }

    /**
     * Устанавливает правильные права доступа для директории
     */
    private static void setPlatformSpecificPermissions(Path dir) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win") && Files.exists(dir) && Files.isDirectory(dir)) {
            // Проверяем, поддерживает ли файловая система POSIX права
            if (dir.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                // Устанавливаем права 700 (rwx------) для Unix-систем
                Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");
                Files.setPosixFilePermissions(dir, permissions);
            }
        }
    }

    /**
     * Получает список всех файлов в директории (рекурсивно)
     */
    public static List<Path> listFilesRecursively(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return Collections.emptyList();
        }

        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(path -> Files.isRegularFile(path))
                    .forEach(files::add);
        }
        return files;
    }

    /**
     * Создает поддиректорию внутри директории приложения
     * @param appName Имя приложения
     * @param subdir Имя поддиректории
     * @return Путь к созданной поддиректории
     */
    public static Path createSubdirectory(String appName, String subdir) throws IOException {
        return getApplicationDataDirectory(appName, subdir);
    }

    /**
     * Создает несколько вложенных поддиректорий
     * @param appName Имя приложения
     * @param subdirs Массив имен поддиректорий
     * @return Путь к самой глубокой поддиректории
     */
    public static Path createNestedSubdirectories(String appName, String... subdirs) throws IOException {
        return getApplicationDataDirectory(appName, subdirs);
    }

    /**
     * Возвращает путь к поддиректории без её создания
     */
    public static Path getSubdirectoryPath(String appName, String... subdirs) throws IOException {
        Path baseDir = determinePlatformSpecificDir(appName);
        Path targetDir = baseDir;

        if (subdirs != null) {
            for (String subdir : subdirs) {
                if (subdir != null && !subdir.trim().isEmpty()) {
                    targetDir = targetDir.resolve(subdir.trim());
                }
            }
        }

        return targetDir;
    }

    public static void main(String[] args) {
        try {
            String appName = "MySuperApp";

            // Создаем основную директорию приложения
            Path appDir = getApplicationDataDirectory(appName);
            System.out.println("Application directory: " + appDir);

            // Создаем поддиректории для разных типов данных
            Path cacheDir = getApplicationDataDirectory(appName, "cache");
            Path logsDir = getApplicationDataDirectory(appName, "logs");
            Path userDataDir = getApplicationDataDirectory(appName, "user_data", "profiles");
            Path tempDir = getApplicationDataDirectory(appName, "temp");

            System.out.println("Cache directory: " + cacheDir);
            System.out.println("Logs directory: " + logsDir);
            System.out.println("User profiles directory: " + userDataDir);
            System.out.println("Temp directory: " + tempDir);

            // Создаем файлы в разных поддиректориях
            Path logFile = logsDir.resolve("application.log");
            Path userProfile = userDataDir.resolve("user123.json");
            Path cacheFile = cacheDir.resolve("data.cache");

            if (!Files.exists(logFile)) {
                Files.createFile(logFile);
                System.out.println("Log file created: " + logFile);
            }

            if (!Files.exists(userProfile)) {
                Files.createFile(userProfile);
                System.out.println("User profile file created: " + userProfile);
            }

            if (!Files.exists(cacheFile)) {
                Files.createFile(cacheFile);
                System.out.println("Cache file created: " + cacheFile);
            }

            // Пример получения пути без создания
            Path configDir = getSubdirectoryPath(appName, "config");
            System.out.println("Config directory (not created yet): " + configDir);

            // Создаем только если нужно
            if (!Files.exists(configDir)) {
                System.out.println("Config directory doesn't exist, creating...");
                Files.createDirectories(configDir);
                setPlatformSpecificPermissions(configDir);
            }

            // Просматриваем содержимое директории приложения
            System.out.println("\nContents of application directory:");
            List<Path> allFiles = listFilesRecursively(appDir);
            for (Path file : allFiles) {
                System.out.println("  - " + appDir.relativize(file));
            }

        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
            e.printStackTrace();

            // Резервный вариант - использовать текущую директорию
            Path fallback = Paths.get(System.getProperty("user.dir"), "app_data");
            System.out.println("Using fallback directory: " + fallback);

            try {
                if (!Files.exists(fallback)) {
                    Files.createDirectories(fallback);
                }
            } catch (IOException fallbackEx) {
                System.err.println("Error creating fallback directory: " + fallbackEx.getMessage());
            }
        }
    }
}