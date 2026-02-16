module com.pancreatitis {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    // Экспортируем основной пакет (если другие модули должны использовать ваши классы)
    exports com.pancreatitis;

    // Открываем пакет с UI-классами (включая Application) для JavaFX
    opens com.pancreatitis.ui to javafx.fxml, javafx.graphics;

    // Если у вас FXML-контроллеры в отдельном пакете — открываем и его
    opens com.pancreatitis.ui.fxml to javafx.fxml;
}