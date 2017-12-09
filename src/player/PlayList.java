package player;

import javafx.beans.Observable;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.LoggerFactory;

import java.io.*;

public class PlayList {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(PlaylistItem.class);
    public static final String KEY_PLAYLIST_ID = "#PLAYLIST_ID";
    public static final String KEY_PLAYLIST_CRONTAB = "#PLAYLIST_CRONTAB";
    public static final String KEY_PLAYLIST_START_TIME = "#PLAYLIST_START_TIME";
    public static final String KEY_PLAYLIST_END_TIME = "#PLAYLIST_END_TIME";

    private SimpleLongProperty id = new SimpleLongProperty(0);
    private SimpleStringProperty name = new SimpleStringProperty("");
    private SimpleStringProperty cronTab = new SimpleStringProperty("");
    private SimpleLongProperty startTime = new SimpleLongProperty(0);
    private SimpleLongProperty endTime = new SimpleLongProperty(0);
    private ObservableList<PlaylistItem> trackPlaylistItems = FXCollections.observableArrayList(playlistItem ->
                                                                        new Observable[] {playlistItem.fileProperty(),
                                                                                          playlistItem.nameProperty(),
                                                                                          playlistItem.durationProperty()});

    public PlayList(long id, String name){
        this.id.set(id);
        this.name.set(name);
    }

    public PlayList(File m3uFile) throws IOException {
        readM3uFile(m3uFile);
    }

    public void readM3uFile(File m3uFile) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(m3uFile), "utf-8"))) {
            trackPlaylistItems.clear();

            name.setValue(m3uFile.getName().substring(0, m3uFile.getName().length() - 4));

            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) continue;
                if (line.startsWith("#")){
                    if (line.toUpperCase().startsWith(KEY_PLAYLIST_ID)){
                        try {
                            id.setValue(Long.parseLong(line.toUpperCase().substring(KEY_PLAYLIST_ID.length() + 1)));
                        }catch (NumberFormatException e){
                            log.error("Can't parse " + KEY_PLAYLIST_ID + " from file " + m3uFile.getAbsolutePath(), e);
                        }
                    }
                    if (line.toUpperCase().startsWith(KEY_PLAYLIST_CRONTAB)){
                        cronTab.setValue(line.toUpperCase().substring(KEY_PLAYLIST_CRONTAB.length() + 1));
                    }
                    if (line.toUpperCase().startsWith(KEY_PLAYLIST_START_TIME)){
                        try {
                            startTime.setValue(Long.parseLong(line.toUpperCase().substring(KEY_PLAYLIST_START_TIME.length() + 1)));
                        }catch (NumberFormatException e){
                            startTime.setValue(0);
                        }
                    }
                    if (line.toUpperCase().startsWith(KEY_PLAYLIST_END_TIME)){
                        try {
                            endTime.setValue(Long.parseLong(line.toUpperCase().substring(KEY_PLAYLIST_END_TIME.length() + 1)));
                        }catch (NumberFormatException e){
                            endTime.setValue(0);
                        }
                    }
                    if ((getCronTab() == null || getCronTab().isEmpty()) && getEndTime() < getStartTime())
                        setEndTime(getStartTime());
                }else {
                    File songFile = new File(m3uFile.getParent() + File.separator + line.trim());
                    trackPlaylistItems.add(new PlaylistItem(songFile));
                }
            }
        }
    }

    public long getId() {
        return id.get();
    }
    public PlayList setId(long id) {
        this.id.set(id);
        return this;
    }
    public SimpleLongProperty getIdProperty(){
        return id;
    }

    public String getName() {
        return name.get();
    }
    public PlayList setName(String name) {
        this.name.set(name);
        return this;
    }
    public SimpleStringProperty getNameProperty(){
        return name;
    }

    public String getCronTab() {
        return cronTab.get();
    }
    public PlayList setCronTab(String cronTab) {
        this.cronTab.set(cronTab);
        return this;
    }
    public SimpleStringProperty getCronTabProperty(){
        return cronTab;
    }

    public long getStartTime() {
        return startTime.get();
    }
    public PlayList setStartTime(long startTime) {
        this.startTime.set(startTime);
        return this;
    }
    public SimpleLongProperty startTimeProperty(){
        return startTime;
    }

    public long getEndTime() {
        return endTime.get();
    }
    public PlayList setEndTime(long endTime) {
        this.endTime.set(endTime);
        return this;
    }
    public SimpleLongProperty endTimeProperty(){
        return endTime;
    }

    public PlayList setTrackPlaylistItems(ObservableList<PlaylistItem> trackPlaylistItems){
        this.trackPlaylistItems = trackPlaylistItems;
        return this;
    }
    public ObservableList<PlaylistItem> getTrackPlaylistItems() {
        return trackPlaylistItems;
    }

    public File savePlaylistAsM3UFile(String path) throws IOException{
        File m3u = new File(path + File.separator + getName() + ".m3u");
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(m3u.getAbsolutePath()), "utf-8"))) {
            m3u.createNewFile();
            StringBuilder sb = new StringBuilder();
            sb.append(KEY_PLAYLIST_ID).append(" ").append(getId()).append("\n");
            if (getCronTab() != null && !getCronTab().isEmpty())
                sb.append(KEY_PLAYLIST_CRONTAB).append(" ").append(getCronTab()).append("\n");
            if (getStartTime() > 0)
                sb.append(KEY_PLAYLIST_START_TIME).append(" ").append(getStartTime()).append("\n");
            if (getEndTime() > 0)
                sb.append(KEY_PLAYLIST_END_TIME).append(" ").append(getEndTime()).append("\n");

            sb.append("\n").append("\n");

            for (PlaylistItem trackName : getTrackPlaylistItems()) {
                sb.append(trackName.getFile().getName()).append("\n");
            }
            bw.write(sb.toString());
        }
        return m3u;
    }
}
