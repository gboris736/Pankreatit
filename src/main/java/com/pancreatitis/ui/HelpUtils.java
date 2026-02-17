package com.pancreatitis.ui;

import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;
import javafx.util.Duration;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class HelpUtils {
    private HelpUtils() {}

    public static <S, T> void attachHelp(TableColumn<S, T> column, Function<T, String> tooltipProvider) {
        if (column == null) return;

        final Callback<TableColumn<S, T>, TableCell<S, T>> baseFactory = column.getCellFactory() != null
                ? column.getCellFactory()
                : col -> new TableCell<>();

        Callback<TableColumn<S, T>, TableCell<S, T>> decoratingFactory = col -> {
            TableCell<S, T> cell = baseFactory.call(col);

            final Tooltip tooltip = new Tooltip();
            tooltip.setShowDelay(Duration.millis(200));
            tooltip.setHideDelay(Duration.millis(100));
            tooltip.setShowDuration(Duration.seconds(10));

            // Добавляем listener на itemProperty / emptyProperty чтобы обновлять tooltip, не ломая поведение клетки
            cell.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    cell.setTooltip(null);
                } else {
                    String tip = tooltipProvider != null ? tooltipProvider.apply(newItem) : (newItem.toString());
                    if (tip == null || tip.isEmpty()) cell.setTooltip(null);
                    else {
                        tooltip.setText(tip);
                        cell.setTooltip(tooltip);
                    }
                }
            });

            // При смене пустоты (например при переиспользовании ячеек) убираем tooltip
            cell.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty) cell.setTooltip(null);
            });

            return cell;
        };

        column.setCellFactory(decoratingFactory);
    }

    public static <S, T> void attachRowAwareHelp(TableColumn<S, T> column, BiFunction<S, T, String> tooltipProvider) {
        if (column == null) return;

        final Callback<TableColumn<S, T>, TableCell<S, T>> baseFactory = column.getCellFactory() != null
                ? column.getCellFactory()
                : col -> new TableCell<>();

        Callback<TableColumn<S, T>, TableCell<S, T>> decoratingFactory = col -> {
            TableCell<S, T> cell = baseFactory.call(col);

            final Tooltip tooltip = new Tooltip();
            tooltip.setShowDelay(Duration.millis(200));
            tooltip.setHideDelay(Duration.millis(100));
            tooltip.setShowDuration(Duration.seconds(10));

            cell.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null || cell.getTableRow() == null || cell.getTableRow().getItem() == null) {
                    cell.setTooltip(null);
                } else {
                    @SuppressWarnings("unchecked")
                    S rowItem = (S) cell.getTableRow().getItem();
                    String tip = tooltipProvider != null ? tooltipProvider.apply(rowItem, newItem) : (newItem == null ? "" : newItem.toString());
                    if (tip == null || tip.isEmpty()) cell.setTooltip(null);
                    else {
                        tooltip.setText(tip);
                        cell.setTooltip(tooltip);
                    }
                }
            });

            cell.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty) cell.setTooltip(null);
            });

            // также обновим tooltip при смене строки (например при скролле)
            cell.tableRowProperty().addListener((obs, oldRow, newRow) -> {
                // форс-обновление текущего item -> сработает listener выше
                T current = cell.getItem();
                if (current != null) cell.itemProperty().setValue(current);
            });

            return cell;
        };

        column.setCellFactory(decoratingFactory);
    }


    // Старое помогало с валидацией вводимых параметров анкеты
    /*public static void attachHelpForAnketChar(TableColumn<?, Integer> column, String template) {
        @SuppressWarnings("unchecked")
        TableColumn<AnketCharViewController.AnketCharacteristicRow, Integer> col =
                (TableColumn<AnketCharViewController.AnketCharacteristicRow, Integer>) column;

        attachRowAwareHelp(col, (row, val) -> {
            try {
                int min = row.getAnketCharacter().getType().minValue();
                int max = row.getAnketCharacter().getType().maxValue();
                return String.format(template, min, max);
            } catch (Exception e) {
                return template;
            }
        });
    }*/

    public static <S, T> void attachHelp(TableColumn<S, T> column, String message) {
        attachHelp(column, item -> item == null ? "" : message);
    }

    public static <S, T> void attachHelp(TableColumn<S, T> column) {
        attachHelp(column, item -> item == null ? "" : item.toString());
    }

    public static void attachHelp(Node node, String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.millis(250));
        t.setHideDelay(Duration.millis(100));
        node.setOnMouseEntered(e -> Tooltip.install(node, t));
        node.setOnMouseExited(e -> Tooltip.uninstall(node, t));
    }
}
