package player;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PlaylistItem {
    private static Logger log = LoggerFactory.getLogger(PlaylistItem.class);

    private SimpleStringProperty name = new SimpleStringProperty("");
    private SimpleObjectProperty<File> file = new SimpleObjectProperty<>(new File(""));
    private SimpleObjectProperty<Duration> duration = new SimpleObjectProperty<>(new Duration(0));

    public PlaylistItem(File file){
        setFile(file);
    }

    public PlaylistItem setFile(File newFile){
        file.setValue(newFile);
        if (newFile == null){
            name.setValue("");
            duration.setValue(new Duration(0));
            return this;
        }
        try {
            if(!file.getValue().exists())
                throw new IOException(file.getValue().getAbsolutePath() + "file is not exists");
            if (!utils.Utils.isMp3(file.getValue()))
                throw new RuntimeException("Not MP3 audio format");

            final MediaPlayer mp = new MediaPlayer(new Media(file.getValue().toURI().toString()));
            mp.setOnReady(() -> {
                Map props = mp.getMedia().getMetadata();
                if (props.containsKey("title") && String.valueOf(props.get("title")).trim().length() > 0)
                    name.setValue(String.valueOf(props.get("title")));
                else
                    name.setValue(newFile.getName().substring(0, newFile.getName().toUpperCase().lastIndexOf(".MP3")));
                duration.setValue(mp.getMedia().getDuration());
            });
        }catch (IOException e){
            name.setValue(newFile.getName() + " (файл не найден!)");
        }catch (RuntimeException e){
            name.setValue(newFile.getName() + " (не верный формат файла!)");
        }
        return this;
    }
    public File getFile() {
        return file.getValue();
    }
    public SimpleObjectProperty<File> fileProperty(){
        return file;
    }


    public String getName() {
        return name.getValue();
    }
    public SimpleStringProperty nameProperty(){
        return name;
    }

    public Duration geDuration(){
        return duration.getValue();
    }
    public SimpleObjectProperty<Duration> durationProperty(){
        return duration;
    }

    public String getTotalRepresentation(){
        long total = (long) duration.getValue().toSeconds();
        if (total > 0){
            int minutes = (int) TimeUnit.SECONDS.toMinutes(total);
            int hours = (int)TimeUnit.SECONDS.toHours(total);
            minutes = minutes - hours * 60;
            int seconds = (int) (duration.getValue().toSeconds() - minutes * 60 - hours * 3600);
            if (hours > 0)
                return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            else
                return String.format("%02d:%02d", minutes, seconds);
        }else {
            return "";
        }
    }

    public boolean isFileCheck() {
        return file.getValue() != null && file.getValue().exists();
    }

    @Override
    public String toString() {
        return name.getValue() + "    (" + getTotalRepresentation() + ")";
    }
}
