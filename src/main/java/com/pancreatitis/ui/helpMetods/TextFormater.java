package com.pancreatitis.ui.helpMetods;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class TextFormater {
    // 1. Double (может быть отрицательным): -123, 12.34, .5, -0.1
    public static Pattern doublePattern = Pattern.compile("^-?[0-9]*\\.?[0-9]*$");

    // 2. Unsigned Double (только положительные): 123, 12.34, .5
    // Просто убираем минус
    public static Pattern udoublePattern = Pattern.compile("^[0-9]*\\.?[0-9]*$");

    // 3. Integer (может быть отрицательным): -123, 0, 456
    // Важно: хотя бы одна цифра должна быть
    public static Pattern intPattern = Pattern.compile("^-?[0-9]+$");

    // 4. Unsigned Integer (только положительные): 0, 123, 456
    public static Pattern uintPattern = Pattern.compile("^[0-9]+$");

    public static void setTextPatternForTextField(TextField textField, Pattern pattern) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (pattern.matcher(newText).matches()) {
                return change;
            }
            return null;
        };
        textField.setTextFormatter(new TextFormatter<>(filter));
    }

}
