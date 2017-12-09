import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.slf4j.LoggerFactory;
import player.MaxPlayer;
import skin_jfx.PlayerSkin;

import javax.swing.*;
import java.awt.*;

public class Main {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (!GraphicsEnvironment.isHeadless())
            new JFXPanel(); //http://stackoverflow.com/questions/11273773/javafx-2-1-toolkit-not-initialized

        MaxPlayer maxPlayer = new MaxPlayer();

        if (!GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        new PlayerSkin(maxPlayer).setVisible(true);
                    }
                });
            });
        }
    }
}
