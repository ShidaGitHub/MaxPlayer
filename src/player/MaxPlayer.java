package player;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Alert;
import javazoom.jlgui.basicplayer.*;
import org.aeonbits.owner.ConfigFactory;
import org.quartz.SchedulerException;
import org.slf4j.LoggerFactory;
import player.client.MaxClient;
import utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MaxPlayer  implements BasicPlayerListener {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(MaxPlayer.class);
    public static final String BACKGROUNDS_PLAYLIST_FILE_NAME = "BACKGROUNDS_PLAYLIST.m3u";

    private Settings mSettings;
    private MaxClient mMaxClient;
    private BasicPlayer mBasicPlayer;

    private IntegerProperty mPlayerStatusProperty = new SimpleIntegerProperty(BasicPlayerEvent.UNKNOWN);

    private ObservableList<PlayList> mPlaylistCollection = FXCollections.observableArrayList();

    private double mBackgroundListVolume;
    private double mTrackListVolume;

    //skin property
    private ObjectProperty<PlaylistItem> mPlayingListItem = new SimpleObjectProperty<>();
    private StringProperty mPlayingFileTimeProperty = new SimpleStringProperty("");
    private DoubleProperty mPlayingFileProgressProperty = new SimpleDoubleProperty(0.00);
    private ObjectProperty<PlayList> mPlayingPlaylist = new SimpleObjectProperty(null);
    private IntegerProperty mPlayingPlaylistPos = new SimpleIntegerProperty(0);
    private IntegerProperty mBackgroundPlaylistPos = new SimpleIntegerProperty(0);

    public MaxPlayer(){
        mSettings = ConfigFactory.create(Settings.class);
        mBasicPlayer = new BasicPlayer();
        mBasicPlayer.addBasicPlayerListener(this);

        mBackgroundListVolume = mSettings.VOLUME_BACKGROUND_PLAYLIST() /100;
        mTrackListVolume = mSettings.VOLUME_TRACK_PLAYLIST() /100;

        initBackgroundPlayList();
        initTrackPlayLists();
        initTempsDir();

        try {
            playTrack(0, mBackgroundPlaylistPos.getValue());
        } catch (BasicPlayerException e) {
            log.error("Can't play background", e);
        }

        try {
            long id = mSettings.PLAYER_ID();
            String serverIp = mSettings.SERVER_IP();
            String serverPort = mSettings.SERVER_PORT();
            mMaxClient = new MaxClient(new URI("ws://" + serverIp + ":" + serverPort + "/" + id), this);
        } catch (SchedulerException | URISyntaxException e) {
            log.error("Can't start web socket", e);
        }
    }

    /**
     * Create new file Settings.properties in home dir
     *
     * @param newProps if newProps contains keys from Settings.class then value will refreshed else will use old value
     */
    public void saveSettings(Properties newProps){
        Properties property = new Properties();

        //PLAYER_ID
        if (newProps.getProperty(Settings.PLAYER_ID_PROPERTY_NAME) == null)
            property.setProperty(Settings.PLAYER_ID_PROPERTY_NAME, String.valueOf(mSettings.PLAYER_ID()));
        else
            property.setProperty(Settings.PLAYER_ID_PROPERTY_NAME, newProps.getProperty(Settings.PLAYER_ID_PROPERTY_NAME));

        //SERVER_IP
        if (newProps.getProperty(Settings.SERVER_IP_PROPERTY_NAME) == null)
            property.setProperty(Settings.SERVER_IP_PROPERTY_NAME, String.valueOf(mSettings.SERVER_IP()));
        else
            property.setProperty(Settings.SERVER_IP_PROPERTY_NAME, newProps.getProperty(Settings.SERVER_IP_PROPERTY_NAME));

        //SERVER_PORT
        if (newProps.getProperty(Settings.SERVER_PORT_PROPERTY_NAME) == null)
            property.setProperty(Settings.SERVER_PORT_PROPERTY_NAME, String.valueOf(mSettings.SERVER_PORT()));
        else
            property.setProperty(Settings.SERVER_PORT_PROPERTY_NAME, newProps.getProperty(Settings.SERVER_PORT_PROPERTY_NAME));

        //TRACKS_DIR
        if (newProps.getProperty(Settings.TRACKS_DIR_PROPERTY_NAME) == null)
            property.setProperty(Settings.TRACKS_DIR_PROPERTY_NAME, String.valueOf(mSettings.TRACKS_DIR()));
        else
            property.setProperty(Settings.TRACKS_DIR_PROPERTY_NAME, newProps.getProperty(Settings.TRACKS_DIR_PROPERTY_NAME));

        //BACKGROUNDS_DIR
        if (newProps.getProperty(Settings.BACKGROUNDS_DIR_PROPERTY_NAME) == null)
            property.setProperty(Settings.BACKGROUNDS_DIR_PROPERTY_NAME, String.valueOf(mSettings.BACKGROUNDS_DIR()));
        else
            property.setProperty(Settings.BACKGROUNDS_DIR_PROPERTY_NAME, newProps.getProperty(Settings.BACKGROUNDS_DIR_PROPERTY_NAME));

        //TEMP_DIR
        if (newProps.getProperty(Settings.TEMP_DIR_PROPERTY_NAME) == null)
            property.setProperty(Settings.TEMP_DIR_PROPERTY_NAME, String.valueOf(mSettings.TEMP_DIR()));
        else
            property.setProperty(Settings.TEMP_DIR_PROPERTY_NAME, newProps.getProperty(Settings.TEMP_DIR_PROPERTY_NAME));

        //VOLUME_TRACK_PLAYLIST_PROPERTY_NAME
        if (newProps.getProperty(Settings.VOLUME_TRACK_PLAYLIST_PROPERTY_NAME) == null)
            property.setProperty(Settings.VOLUME_TRACK_PLAYLIST_PROPERTY_NAME, String.valueOf(mSettings.VOLUME_TRACK_PLAYLIST()));
        else
            property.setProperty(Settings.VOLUME_TRACK_PLAYLIST_PROPERTY_NAME, newProps.getProperty(Settings.VOLUME_TRACK_PLAYLIST_PROPERTY_NAME));

        //VOLUME_BACKGROUND_PLAYLIST_PROPERTY_NAME
        if (newProps.getProperty(Settings.VOLUME_BACKGROUND_PLAYLIST_PROPERTY_NAME) == null)
            property.setProperty(Settings.VOLUME_BACKGROUND_PLAYLIST_PROPERTY_NAME, String.valueOf(mSettings.VOLUME_BACKGROUND_PLAYLIST()));
        else
            property.setProperty(Settings.VOLUME_BACKGROUND_PLAYLIST_PROPERTY_NAME, newProps.getProperty(Settings.VOLUME_BACKGROUND_PLAYLIST_PROPERTY_NAME));

        //CHECK_UPDATE_SCHEDULE
        if (newProps.getProperty(Settings.CHECK_UPDATE_SCHEDULE_PROPERTY_NAME) == null)
            property.setProperty(Settings.CHECK_UPDATE_SCHEDULE_PROPERTY_NAME, String.valueOf(mSettings.CHECK_UPDATE_SCHEDULE()));
        else
            property.setProperty(Settings.CHECK_UPDATE_SCHEDULE_PROPERTY_NAME, newProps.getProperty(Settings.CHECK_UPDATE_SCHEDULE_PROPERTY_NAME));


        try (OutputStream output = new FileOutputStream(System.getProperty("user.dir") + File.separator + Settings.SETTINGS_FILE_NAME)) {
            property.store(output, null);
            mSettings.reload();
        }catch (IOException exIO){
            String msg = "Can't create Settings . \nCheck " +
                    (new File(System.getProperty("user.dir") + File.separator + "Settings.properties")).getAbsolutePath();
            if (!GraphicsEnvironment.isHeadless()) {
                Utils.getStylingAlert(Alert.AlertType.ERROR, "Внимание", msg, "").showAndWait();
            }
            log.error(msg, exIO);
            System.exit(0);
        }

    }

    /**
     *  Load mp3 files from BACKGROUNDS_PLAYLIST_FILE_NAME(playlist try find in dir BACKGROUNDS_DIR property from Settings).
     *  Check and create background playlist dir. If background playlist is empty then add all mp3 files from background playlist dir.
     */
    private void initBackgroundPlayList(){
        //check backgrounds dir
        File backgroundsDir;
        try {
            backgroundsDir = new File(mSettings.BACKGROUNDS_DIR());
            if (!backgroundsDir.exists() || !backgroundsDir.isDirectory())
                throw new NullPointerException();
        }catch (NullPointerException ex){
            backgroundsDir = new File(System.getProperty("user.dir") + File.separator + Settings.BACKGROUNDS_DIR_PROPERTY_NAME);
            if (!backgroundsDir.exists()){
                backgroundsDir.mkdirs();
            }
            Properties newProps = new Properties();
            newProps.setProperty(Settings.BACKGROUNDS_DIR_PROPERTY_NAME, backgroundsDir.getAbsolutePath());
            saveSettings(newProps);
        }

        // check BACKGROUNDS_PLAYLIST.m3u
        File backgroundPlaylist;
        try {
            backgroundPlaylist = new File(mSettings.BACKGROUNDS_DIR() + File.separator + BACKGROUNDS_PLAYLIST_FILE_NAME);
            if (!backgroundPlaylist.exists() || backgroundPlaylist.isDirectory())
                throw new NullPointerException();
        }catch (NullPointerException ex){
            backgroundPlaylist = new File(mSettings.BACKGROUNDS_DIR() + File.separator + BACKGROUNDS_PLAYLIST_FILE_NAME);
            if (!backgroundPlaylist.exists()){
                try {
                    backgroundPlaylist.createNewFile();
                }catch (IOException exx){
                    String msg = "Can't create play list . \nCheck " + backgroundPlaylist.getAbsolutePath();
                    if (!GraphicsEnvironment.isHeadless()) {
                        JOptionPane.showMessageDialog(null, msg, "Exception", JOptionPane.ERROR_MESSAGE);
                    }
                    log.error(msg, exx);
                    return;
                }
            }
        }
        reloadBackgroundPlayList();
    }

    /**
     */
    private void initTrackPlayLists(){
        //check tracks dir
        File tracksDir;
        try {
            tracksDir = new File(mSettings.TRACKS_DIR());
            if (!tracksDir.exists() || !tracksDir.isDirectory())
                throw new NullPointerException();
        }catch (NullPointerException ex){
            tracksDir = new File(System.getProperty("user.dir") + File.separator + Settings.TRACKS_DIR_PROPERTY_NAME);
            if (!tracksDir.exists()){
                tracksDir.mkdirs();
            }
            Properties newProps = new Properties();
            newProps.setProperty(Settings.TRACKS_DIR_PROPERTY_NAME, tracksDir.getAbsolutePath());
            saveSettings(newProps);
        }

        File[] M3UFiles = tracksDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".m3u"));
        for (File m3u : M3UFiles) {
            try {
                mPlaylistCollection.add(new PlayList(m3u));
            } catch (IOException e) {
                log.error("Can't load m3u " + m3u.getAbsolutePath());
            }
        }
    }

    private void initTempsDir(){
        File tempsDir;
        try {
            tempsDir = new File(mSettings.TEMP_DIR());
            if (!tempsDir.exists() || !tempsDir.isDirectory())
                throw new NullPointerException();
        }catch (NullPointerException ex){
            tempsDir = new File(System.getProperty("user.dir") + File.separator + Settings.TEMP_DIR_PROPERTY_NAME);
            if (!tempsDir.exists()){
                tempsDir.mkdirs();
            }
            Properties newProps = new Properties();
            newProps.setProperty(Settings.TEMP_DIR_PROPERTY_NAME, tempsDir.getAbsolutePath());
            saveSettings(newProps);
        }
        File bait = new File(mSettings.TEMP_DIR() + File.separator + "4715154.bait");//http://bugs.java.com/view_bug.do?bug_id=4715154
        if (bait.exists()) bait.delete();
    }

    /**
     *  Load mp3 files from BACKGROUNDS_PLAYLIST_FILE_NAME(playlist try find in dir BACKGROUNDS_DIR property from Settings).
     */
    private void reloadBackgroundPlayList(){
        File m3u = new File(mSettings.BACKGROUNDS_DIR() + File.separator + BACKGROUNDS_PLAYLIST_FILE_NAME);
        try {
            PlayList backgroundPlaylist = getPlaylistOnId(0l);
            if (backgroundPlaylist == null){
                backgroundPlaylist = new PlayList(m3u);
            }else {
                mPlaylistCollection.remove(backgroundPlaylist);
                backgroundPlaylist.readM3uFile(m3u);
            }

            if (backgroundPlaylist.getTrackPlaylistItems().size() == 0){
                File backDir = new File(mSettings.BACKGROUNDS_DIR());
                File[] mp3Files = backDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
                for (File file : mp3Files)
                    backgroundPlaylist.getTrackPlaylistItems().add(new PlaylistItem(file));
                backgroundPlaylist.savePlaylistAsM3UFile(mSettings.BACKGROUNDS_DIR());
            }
            mPlaylistCollection.add(0, backgroundPlaylist);
        }catch (IOException ex){
            log.error("Can't load background playlist");
        }
    }

    public void reloadTrackPlayListCollection(){
        mPlaylistCollection.clear();

        File backsDir = new File(mSettings.BACKGROUNDS_DIR());
        File[] M3UFilesBacksDir = backsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".m3u"));
        for (File m3u : M3UFilesBacksDir) {
            try {
                mPlaylistCollection.add(new PlayList(m3u));
            } catch (IOException e) {
                log.error("Can't load m3u " + m3u.getAbsolutePath());
            }
        }

        File tracksDir = new File(mSettings.TRACKS_DIR());
        File[] M3UFiles = tracksDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".m3u"));
        for (File m3u : M3UFiles) {
            try {
                mPlaylistCollection.add(new PlayList(m3u));
            } catch (IOException e) {
                log.error("Can't load m3u " + m3u.getAbsolutePath());
            }
        }
    }

    public void reloadPlayList(long id){
        if (id == 0l){ //BackgroundPlayList
            reloadBackgroundPlayList();
            return;
        }

        PlayList playList = getPlaylistOnId(id);
        if (playList != null){
            File tracksDir = new File(mSettings.TRACKS_DIR());
            File[] M3UFiles = tracksDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".m3u"));
            for (File m3u : M3UFiles) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(m3u), "utf-8"))) {
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().length() == 0) continue;
                        if (line.startsWith("#")) {
                            if (line.toUpperCase().startsWith(PlayList.KEY_PLAYLIST_ID)){
                                try {
                                    long m3uId = Long.parseLong(line.toUpperCase().substring(PlayList.KEY_PLAYLIST_ID.length() + 1));
                                    if (m3uId == playList.getId()){
                                        playList.readM3uFile(m3u);
                                        break;
                                    }
                                }catch (NumberFormatException e){
                                    log.error("Can't parse " + PlayList.KEY_PLAYLIST_ID + " from file " + m3u.getAbsolutePath(), e);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    log.error("Can't load m3u " + m3u.getAbsolutePath());
                }
            }
        }
    }

    public ObservableList<PlayList> getPlaylistCollection(){
        return mPlaylistCollection;
    }

    @Override
    public void opened(Object o, Map map) {
        if (o instanceof File){
            mPlayingPlaylist.getValue().getTrackPlaylistItems().stream().
                    filter(pli -> pli.getFile().equals((File) o)).forEachOrdered(pli -> mPlayingListItem.setValue(pli));
        }
    }

    @Override
    public void progress(int i, long l, byte[] bytes, Map map) {
        try {
            Platform.runLater(() -> {
                if (mBasicPlayer.getStatus() == BasicPlayer.STOPPED || mPlayerStatusProperty.getValue() == BasicPlayer.UNKNOWN) {
                    mPlayingFileTimeProperty.setValue("00:00");
                    mPlayingFileProgressProperty.setValue(0);
                } else {
                    long progressTime = (long) map.get("mp3.position.microseconds") / 1000000;
                    int minutes = (int) TimeUnit.SECONDS.toMinutes(progressTime);
                    int hours = (int) TimeUnit.SECONDS.toHours(progressTime);
                    minutes = minutes - hours * 60;
                    int seconds = (int) (progressTime - minutes * 60 - hours * 3600);
                    if (hours > 0)
                        mPlayingFileTimeProperty.setValue(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    else
                        mPlayingFileTimeProperty.setValue(String.format("%02d:%02d", minutes, seconds));

                    mPlayingFileProgressProperty.setValue(Double.valueOf(TimeUnit.SECONDS.toSeconds(progressTime)) /
                            Double.valueOf(mPlayingListItem.getValue().geDuration().toSeconds()));
                }
            });
        }catch (IllegalStateException ex){}
    }

    @Override
    public void stateUpdated(BasicPlayerEvent bpe) {
        mPlayerStatusProperty.setValue(bpe.getCode());

        if (bpe.getCode() == BasicPlayerEvent.EOM) { //end track
            if (mPlayingPlaylist.getValue().getId() != 0){ //simple  playlist
                if (mPlayingPlaylistPos.getValue() + 1 < mPlayingPlaylist.getValue().getTrackPlaylistItems().size()) { //play next track of playlist
                    mPlayingPlaylistPos.setValue(mPlayingPlaylistPos.getValue() + 1);
                    try {
                        playTrack(mPlayingPlaylist.getValue().getId(), mPlayingPlaylistPos.getValue());
                    } catch (BasicPlayerException e) {
                        log.error("Can't play playlist " + mPlayingPlaylist.getName(), e);
                    }
                } else { //play background
                    mPlayingPlaylistPos.setValue(0);
                    try {
                        playTrack(0, mBackgroundPlaylistPos.getValue());
                    } catch (BasicPlayerException e) {
                        log.error("Can't play playlist " + mPlayingPlaylist.getName(), e);
                    }
                }
            }else { //background playlist
                if (mBackgroundPlaylistPos.getValue() + 1 < mPlayingPlaylist.getValue().getTrackPlaylistItems().size()) { //play next track of background playlist
                    mBackgroundPlaylistPos.setValue(mBackgroundPlaylistPos.getValue() + 1);
                }else {
                    mBackgroundPlaylistPos.setValue(0);
                }
                try {
                    playTrack(0, mBackgroundPlaylistPos.getValue());
                } catch (BasicPlayerException e) {
                    log.error("Can't play playlist " + mPlayingPlaylist.getName(), e);
                }
            }
        }
    }

    @Override
    public void setController(BasicController basicController) {

    }

    public PlayList getPlaylistOnId(long id){
        FilteredList<PlayList> filteredData = new FilteredList<>(mPlaylistCollection, p -> true);
        filteredData.setPredicate(playList -> playList.getId() == id);
        if (filteredData.size() > 0){
            return filteredData.get(0);
        }
        return null;
    }

    /**
     * Check playList contains file. Compare file name
     *
     * @return LinkedList playList's id (if background playList contains file id = 0)
     **/
    public LinkedList<Long> fileInPlaylist(File file){
        LinkedList<Long> playListCollection = new LinkedList<>();
        for (PlayList pl : mPlaylistCollection){
            playListCollection.addAll(pl.getTrackPlaylistItems().stream().
                    filter(pli -> pli.getFile().getName().equals(file.getName()) && !playListCollection.contains(pl.getId())).
                    map(pli -> pl.getId()).collect(Collectors.toList()));
        }
        return playListCollection;
    }

    public void stopPlayer() throws BasicPlayerException{
        mBasicPlayer.stop();
    }

    public void playTrack(long playlistId, int pos) throws BasicPlayerException {
        FilteredList<PlayList> filteredData = new FilteredList<>(mPlaylistCollection, p -> true);
        filteredData.setPredicate(playList -> playList.getId() == playlistId);
        if (filteredData.size() > 0){
            PlayList playlist = filteredData.get(0);
            if (playlist.getTrackPlaylistItems().size() > pos && playlist.getTrackPlaylistItems().get(pos).isFileCheck()){
                stopPlayer();

                mPlayingPlaylist.setValue(playlist);
                mPlayingPlaylistPos.setValue(pos);

                mBasicPlayer.open(playlist.getTrackPlaylistItems().get(pos).getFile());
                mBasicPlayer.play();
                if (playlistId == 0)
                    mBasicPlayer.setGain(mBackgroundListVolume);
                else
                    mBasicPlayer.setGain(mTrackListVolume);
            }
        }

    }

    public ObjectProperty<PlayList> getPlayingPlaylist() {
        return mPlayingPlaylist;
    }

    public IntegerProperty getPlayingPlaylistPos() {
        return mPlayingPlaylistPos;
    }
    public IntegerProperty getBackgroundPlaylistPos() {
        return mBackgroundPlaylistPos;
    }
    public ObjectProperty<PlaylistItem> getPlayingListItemProperty(){
        return  mPlayingListItem;
    }
    public DoubleProperty getPlayingFileProgressProperty(){
        return mPlayingFileProgressProperty;
    }
    public StringProperty getPlayingFileTimeProperty(){
        return mPlayingFileTimeProperty;
    }

    public double getTrackListVolume() {
        return mTrackListVolume;
    }
    public double getBackgroundListVolume() {
        return mBackgroundListVolume;
    }

    public void setTrackListVolume(double newVolume) {
        mTrackListVolume = newVolume;
        try {
            if (mBasicPlayer.getStatus() == BasicPlayer.PLAYING && mPlayingPlaylist.getValue().getId() != 0)
                mBasicPlayer.setGain(mTrackListVolume);
        } catch (BasicPlayerException ex) {log.error("can't change volume: ", ex);}
    }
    public void setBackgroundListVolume(double newVolume) {
        mBackgroundListVolume = newVolume;
        try {
            if (mBasicPlayer.getStatus() == BasicPlayer.PLAYING && mPlayingPlaylist.getValue().getId() == 0)
                mBasicPlayer.setGain(mBackgroundListVolume);
        } catch (BasicPlayerException ex) {log.error("can't change volume: ", ex);}
    }
}
