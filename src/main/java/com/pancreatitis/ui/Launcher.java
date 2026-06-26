package com.pancreatitis.ui;

import com.pancreatitis.modules.initialization.AppInitializer;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // Устанавливаем daemon-потоки для JavaFX
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // ignore
        });

        // Инициализация папок и файлов приложения
        AppInitializer.initialize();

        // Запуск JavaFX-приложения
        Application.launch(StartApplication.class, args);

        // После завершения JavaFX - принудительно выходим
        System.exit(0);
    }
}