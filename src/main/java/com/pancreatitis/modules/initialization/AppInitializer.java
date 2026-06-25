package com.pancreatitis.modules.initialization;

import com.pancreatitis.modules.localstorage.DiskStorageControl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Инициализация структуры папок и файлов приложения.
 * Копирует ресурсы из classpath (папка assets) в папку приложения при их отсутствии.
 */
public class AppInitializer {

    public static void initialize() {
        try {
            DiskStorageControl storage = DiskStorageControl.getInstance();
            Path appDir = storage.getAppDir();
            Path dbPath = storage.getDBPath();
            Path algDir = storage.getAlgDir();

            // Создаём папку users
            Path usersDir = appDir.resolve("users");
            if (!Files.exists(usersDir)) {
                Files.createDirectories(usersDir);
                System.out.println("Created users directory: " + usersDir);
            }

            // Копируем файлы из assets
            copyResourceIfMissing("/assets/pancreatitis_v7.db", dbPath);
            copyResourceIfMissing("/assets/algorithm_2026_04_24_10_45_18.txt", algDir.resolve("algorithm_2026_04_24_10_45_18.txt"));
            copyResourceIfMissing("/assets/wordlist_en.txt", appDir.resolve("wordlist_en.txt"));
            copyResourceIfMissing("/assets/ed25519_private.key", appDir.resolve("ed25519_private.key"));
            copyResourceIfMissing("/assets/ed25519_public.key", appDir.resolve("ed25519_public.key"));

            // Создаём папку dr_roman внутри users и копируем ключи
            Path drRomanDir = usersDir.resolve("dr_roman");
            if (!Files.exists(drRomanDir)) {
                Files.createDirectories(drRomanDir);
                System.out.println("Created dr_roman directory: " + drRomanDir);
            }
            copyResourceIfMissing("/assets/dr_roman/key_admin.enc", drRomanDir.resolve("key_admin.enc"));
            copyResourceIfMissing("/assets/dr_roman/key_user.enc", drRomanDir.resolve("key_user.enc"));

        } catch (Exception e) {
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
            return;
        }

        try (InputStream in = AppInitializer.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Path parent = targetPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.copy(in, targetPath);
        }
    }
}