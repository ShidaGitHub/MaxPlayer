package skin_jfx;


import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import player.MaxPlayer;

import javax.swing.*;
import java.io.IOException;

public class PlayerSkin extends JFrame {
    public static final String LIGHT_STYLE_SHEET = "MainTheme.css";
    public static final String STYLE_SHEET = LIGHT_STYLE_SHEET;

    MaxPlayer mMaxPlayer;

    public PlayerSkin(MaxPlayer maxPlayer){
        setSize(760, 550);
        initComponents();

        mMaxPlayer = maxPlayer;
    }

    private void initComponents(){
        final JFXPanel fxPanel = new JFXPanel();
        add(fxPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Platform.runLater(() -> initFX(fxPanel));
    }

    private void initFX(JFXPanel fxPanel) {
        try {
            FXMLLoader loader = new FXMLLoader(PlayerSkin.class.getResource("Player.fxml"));
            Parent root = loader.load();
            root.getStylesheets().add(PlayerSkin.class.getResource(STYLE_SHEET).toExternalForm());
            PlayerController playerSkinController = loader.getController();
            playerSkinController.setMaxPlayerApp(mMaxPlayer);
            Scene scene = new Scene(root);
            fxPanel.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
