package com.pancreatitis.ui;

import com.pancreatitis.modules.initialization.AppInitializer;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // Инициализация папок и файлов приложения
        AppInitializer.initialize();
        // Запуск JavaFX-приложения
        Application.launch(StartApplication.class, args);
    }
}