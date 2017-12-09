package utils;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import player.PlaylistItem;
import skin_jfx.PlayerSkin;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static File lastDir = new File(System.getProperty("user.home"));

    public static Alert getStylingAlert(Alert.AlertType type, String title, String header, String content){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(PlayerSkin.class.getResource("MainTheme.css").toExternalForm());
        dialogPane.getStyleClass().add("alertDialog");

        return alert;
    }

    public static String getTotalRepresentation(long total){
        String totalRepresentation = "";
        if (total > 0){
            int minutes = (int) TimeUnit.SECONDS.toMinutes(total);
            int hours = (int)TimeUnit.SECONDS.toHours(total);
            minutes = minutes - hours * 60;
            int seconds = (int) (total - minutes * 60 - hours * 3600);
            if (hours > 0)
                totalRepresentation =  String.format("%02d:%02d:%02d", hours, minutes, seconds);
            else
                totalRepresentation = String.format("%02d:%02d", minutes, seconds);
        }
        return totalRepresentation;
    }

    /**
     * Check what file have extension MP3
     *
     * @param file - checked file
     */
    public static boolean isMp3(File file) {
        return file.getName().substring(file.getName().lastIndexOf(".") + 1, file.getName().length()).equalsIgnoreCase("mp3");
    }

    public static void initNumberRowColumn(TableColumn numberCol){
        numberCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Object, Object>, ObservableValue<Object>>() {
            @Override
            public ObservableValue<Object> call(TableColumn.CellDataFeatures<Object, Object> p) {
                return new ReadOnlyObjectWrapper(p.getValue());
            }
        });
        numberCol.setCellFactory(new Callback<TableColumn<Object, Object>, TableCell<Object, Object>>() {
            @Override
            public TableCell<Object, Object> call(TableColumn<Object, Object> param) {
                return new TableCell<Object, Object>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (this.getTableRow() != null && item != null) {
                            setText(String.valueOf(getIndex() + 1));
                        }else
                            setText(null);
                    }
                };
            }
        });
    }

    public static void initNamePlayListItemColumn(TableColumn<PlaylistItem, String> nameCol){
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(param -> new TableCellName());
    }

    public static void initDurationPlayListItemColumn(TableColumn<PlaylistItem, String> durationCol){
        durationCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<PlaylistItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<PlaylistItem, String> param) {
                StringBinding total = new StringBinding() {
                    {
                        super.bind(param.getValue().durationProperty());
                    }
                    @Override
                    protected String computeValue() {
                        return param.getValue().getTotalRepresentation();
                    }
                };
                return total;
            }
        });
        durationCol.setCellFactory(TextFieldTableCell.forTableColumn());
    }

    public static void initFileColumn(TableColumn<PlaylistItem, File> column){
        column.setCellValueFactory(new PropertyValueFactory<>("file"));
        column.setCellFactory(param -> new TableCellFile());
        column.setOnEditCommit((TableColumn.CellEditEvent<PlaylistItem, File> t) -> {
            t.getRowValue().setFile(t.getNewValue().equals(new File("")) ? null : t.getNewValue());
        });
    }

    static class TableCellFile extends TableCell<PlaylistItem, File> {
        GridPane graphic = new GridPane();
        private TextField textField;
        private Button openDialogButton;

        public TableCellFile() {}

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();

                setStyle("-fx-padding: 0.01;");
                createTextField();
                setText(null);
                setGraphic(graphic);
                textField.selectAll();
                textField.requestFocus();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();

            setText(getString());
            setStyle("-fx-padding: 0.8em 0em 0.5em 0.3em;");
            setGraphic(null);
        }

        @Override
        public void updateItem(File item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setStyle("-fx-padding: 0.8em 0em 0.5em 0.3em;");
                    if (getTableRow().getItem() != null && !((PlaylistItem)getTableRow().getItem()).isFileCheck())
                        setStyle("-fx-text-fill: red");
                    else
                        setStyle("-fx-text-fill: black");
                    setText(getString());
                    setGraphic(null);
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            openDialogButton = new Button();

            graphic = new GridPane();
            graphic.setLayoutX(1.0);
            graphic.setLayoutY(1.0);
            graphic.setHgap(0.0);
            ColumnConstraints txtComponent = new ColumnConstraints();
            txtComponent.setHalignment(HPos.RIGHT);
            txtComponent.fillWidthProperty();
            txtComponent.setHgrow(Priority.ALWAYS);
            graphic.getColumnConstraints().add(txtComponent);
            ColumnConstraints closeBtn = new ColumnConstraints();
            closeBtn.setHalignment(HPos.LEFT);
            graphic.getColumnConstraints().add(closeBtn);
            graphic.add(textField, 0, 0);
            graphic.add(openDialogButton, 1, 0);
            graphic.setMinSize(10, 10);
            graphic.setPrefSize(this.getWidth() - this.getGraphicTextGap()* 2, this.getHeight());
            graphic.setMaxSize(5000, 5000);

            graphic.focusedProperty().addListener((arg0, arg1, arg2) -> {
                File chFile = new File(textField.getText());
                if (!arg2 && checkFile(chFile))
                    commitEdit(chFile);
            });

            textField.setMinSize(10, this.getHeight());
            textField.setPrefSize(this.getWidth() - this.getGraphicTextGap()* 2 - 45, this.getHeight());
            textField.setMaxSize(5000, this.getHeight());
            openDialogButton.setStyle("-fx-padding: 0;" +
                    "-fx-border-color: transparent, black;" +
                    "-fx-border-width: 1, 1;" +
                    "-fx-border-style: solid, segments(1, 2);" +
                    "-fx-border-radius: 0, 0;" +
                    "-fx-border-insets: 1 1 1 1, 0;");

            openDialogButton.setMinSize(50, this.getHeight());
            SVGPath icon = new SVGPath();
            icon.setContent("M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z");
            icon.setFill(Color.BLUE);
            openDialogButton.setGraphic(icon);
            openDialogButton.setOnAction(event -> {
                final FileChooser fileChooser = new FileChooser();
                fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("MP3", "*.mp3"));
                fileChooser.setInitialDirectory(lastDir);

                File file = fileChooser.showOpenDialog(null);
                if (file != null){
                    lastDir = file.getParentFile();
                    if (checkFile(file))
                        commitEdit(file);
                }
            });

            textField.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case ENTER:
                        File chFile = new File(textField.getText());
                        if (checkFile(chFile))
                            commitEdit(chFile); break;
                    case F4:
                        openDialogButton.fire();
                    case ESCAPE:  cancelEdit(); break;
                }
            });
        }

        private String getString() {
            return getItem() == null || !getItem().exists() ? "" : getItem().getAbsolutePath();
        }

        private boolean checkFile(File file){
            if (file == null || file.equals(new File(""))){
                return true;
            }
            try {
                if (file != null && !file.exists())
                    throw new IOException("Файл " + file.getAbsolutePath() + " не найден в системе!");
                if (!isMp3(file))
                    throw new UnsupportedAudioFileException("Файл "  + file.getName() +" не имеет MP3 аудио формат");
                return true;
            } catch (UnsupportedAudioFileException | IOException e) {
                Utils.getStylingAlert(Alert.AlertType.ERROR, "Внимание", e.getMessage(), "").show();
            }
            return false;
        }
    }

    static class TableCellName extends TableCell<PlaylistItem, String> {
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (getTableRow().getItem() != null && !((PlaylistItem)getTableRow().getItem()).isFileCheck())
                    setStyle("-fx-text-fill: red");
                else
                    setStyle("-fx-text-fill: black");
                setText(item);
                setGraphic(null);
            }
        }
    }
}

