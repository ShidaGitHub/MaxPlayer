package utils;

import javafx.geometry.HPos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class GroupTextFieldButton extends Group {
    private TextField mTextField = null;
    private Button mButton = null;

    public GroupTextFieldButton(String textFieldText) {
        mTextField = new TextField();
        
        Button openDialogButton = new Button("...");

        GridPane grid = new GridPane();
        grid.setLayoutX(1.0);
        grid.setLayoutY(1.0);
        grid.setHgap(0.0);
        ColumnConstraints txtComponent = new ColumnConstraints();
        txtComponent.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().add(txtComponent);
        ColumnConstraints closeBtn = new ColumnConstraints();
        closeBtn.setHalignment(HPos.LEFT);
        grid.getColumnConstraints().add(closeBtn);
        grid.add(mTextField, 0, 0);
        grid.add(openDialogButton, 1, 0);

        Rectangle focusBorder = new Rectangle();
        focusBorder.setFill(null);
        focusBorder.setStrokeWidth(2.0);
        focusBorder.setStroke(Color.BLACK);
        focusBorder.widthProperty().bind(grid.widthProperty().add(2));
        focusBorder.heightProperty().bind(grid.heightProperty().add(2));
        focusBorder.visibleProperty().bind(mTextField.focusedProperty());

        this.getChildren().addAll(focusBorder, grid);
    }
}
