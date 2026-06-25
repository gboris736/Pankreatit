package com.pancreatitis.modules.initialization;

import com.pancreatitis.modules.localstorage.DiskStorageControl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Инициализация структуры папок и файлов приложения.
 * Копирует ресурсы из classpath в папку приложения при их отсутствии.
 */
public class AppInitializer {

    public static void initialize() {
        try {
            // Получаем пути через существующий модуль хранения
            DiskStorageControl storage = DiskStorageControl.getInstance();
            Path appDir = storage.getAppDir();
            Path dbPath = storage.getDBPath();
            Path algDir = storage.getAlgDir();

            // Создаём папку users (она не создаётся автоматически)
            Path usersDir = appDir.resolve("users");
            if (!Files.exists(usersDir)) {
                Files.createDirectories(usersDir);
                System.out.println("Created users directory: " + usersDir);
            }

            // Копируем файлы базы данных
            copyResourceIfMissing("/dataBaseStor/pancreatitis_v7.db", dbPath);

            // Копируем файл алгоритма
            Path algFile = algDir.resolve("algorithm_2026_04_24_10_45_18.txt");
            copyResourceIfMissing("/appAlgStor/algorithm_2026_04_24_10_45_18.txt", algFile);

            // Копируем файлы в корень приложения
            copyResourceIfMissing("/wordlist_en.txt", appDir.resolve("wordlist_en.txt"));
            copyResourceIfMissing("/ed25519_private.key", appDir.resolve("ed25519_private.key"));
            copyResourceIfMissing("/ed25519_public.key", appDir.resolve("ed25519_public.key"));

            // Создаём папку dr_roman внутри users и копируем ключи
            Path drRomanDir = usersDir.resolve("dr_roman");
            if (!Files.exists(drRomanDir)) {
                Files.createDirectories(drRomanDir);
                System.out.println("Created dr_roman directory: " + drRomanDir);
            }
            copyResourceIfMissing("/users/dr_roman/key_admin.enc", drRomanDir.resolve("key_admin.enc"));
            copyResourceIfMissing("/users/dr_roman/key_user.enc", drRomanDir.resolve("key_user.enc"));

            System.out.println("Application initialization completed successfully.");

        } catch (Exception e) {
            System.err.println("Fatal error during application initialization: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Initialization failed", e);
        }
    }

    /**
     * Копирует ресурс в целевой файл, если тот ещё не существует.
     * Если ресурс отсутствует – выбрасывает исключение.
     */
    private static void copyResourceIfMissing(String resourcePath, Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            return; // уже есть – пропускаем
        }

        try (InputStream in = AppInitializer.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            // Создаём родительские каталоги при необходимости
            Path parent = targetPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.copy(in, targetPath);
            System.out.println("Copied: " + resourcePath + " -> " + targetPath);
        }
    }
}