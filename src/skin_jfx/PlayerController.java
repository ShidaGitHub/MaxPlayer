package skin_jfx;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import org.aeonbits.owner.ConfigFactory;
import org.slf4j.LoggerFactory;
import player.MaxPlayer;
import player.PlayList;
import player.PlaylistItem;
import player.Settings;
import utils.Utils;

import java.io.*;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PlayerController implements Initializable {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(PlaylistItem.class);

    MaxPlayer mMaxPlayer;

    @FXML
    private TabPane playerTabPane;
    @FXML
    private TableView<PlaylistItem> backgroundTable;
    @FXML
    private TableColumn colBackgroundRowNumber;
    @FXML
    private TableColumn<PlaylistItem, String> colBackgroundName, colBackgroundTotal;
    @FXML
    private TableColumn<PlaylistItem, File> colBackgroundFile;
    @FXML
    private Label backgroundPlayListInfo, trackInfo, playTime;
    @FXML
    private ProgressBar playProgress;
    @FXML
    private Slider trackPlaylistVolume, backgroundPlaylistVolume;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //background table
        Utils.initNumberRowColumn(colBackgroundRowNumber);
        Utils.initNamePlayListItemColumn(colBackgroundName);
        Utils.initDurationPlayListItemColumn(colBackgroundTotal);
        Utils.initFileColumn(colBackgroundFile);

        trackPlaylistVolume.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mMaxPlayer != null)
                mMaxPlayer.setTrackListVolume((double) newValue / 100);
        });
        backgroundPlaylistVolume.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mMaxPlayer != null)
                mMaxPlayer.setBackgroundListVolume((double) newValue / 100);
        });

        //player TabPane
        playerTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.getId() != null && newValue.getId().matches("backgroundTab"))
                checkEmptyBackgroundPlayList();
        });
    }

    public void setMaxPlayerApp(MaxPlayer maxPlayerApp){
        mMaxPlayer = maxPlayerApp;

        //playing info
        if (mMaxPlayer.getPlayingListItemProperty().getValue() != null)
            trackInfo.setText(mMaxPlayer.getPlayingListItemProperty().getValue().toString());
        mMaxPlayer.getPlayingListItemProperty().addListener((observable, oldValue, newValue) ->
                                                        Platform.runLater(() -> trackInfo.setText(newValue.toString())));
        //play progress
        playProgress.progressProperty().bind(mMaxPlayer.getPlayingFileProgressProperty());
        //play time
        playTime.textProperty().bind(mMaxPlayer.getPlayingFileTimeProperty());
        //volume
        trackPlaylistVolume.setValue(mMaxPlayer.getTrackListVolume() * 100);
        backgroundPlaylistVolume.setValue(mMaxPlayer.getBackgroundListVolume() * 100);

        //background tab
        ObservableList<PlaylistItem> backgroundItems = mMaxPlayer.getPlaylistCollection().get(0).getTrackPlaylistItems();
        backgroundTable.setItems(backgroundItems);
        backgroundItems.addListener(new ListChangeListener<PlaylistItem>() {
            @Override
            public void onChanged(Change<? extends PlaylistItem> c) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        Long total = backgroundItems.stream().collect(Collectors.summingLong(value ->
                                                                (long) value.durationProperty().getValue().toSeconds()));
                        backgroundPlayListInfo.setText(String.format("Треков в плейлисте:  %d  •  %s", backgroundItems.size(),
                                                                                utils.Utils.getTotalRepresentation(total)));
                    }
                });
            }
        });

        //tabs
        for (PlayList playList : mMaxPlayer.getPlaylistCollection()){
            if (playList.getId() == 0) //background
                continue;

            FilteredList<Tab> filteredData = new FilteredList<>(playerTabPane.getTabs(), p -> true);
            filteredData.setPredicate(tab -> tab.getText().equals(playList.getName()));
            playerTabPane.getTabs().removeAll(filteredData);
            addNewTab(playList);
        }

        mMaxPlayer.getPlaylistCollection().addListener(new ListChangeListener<PlayList>() {
            @Override
            public void onChanged(Change<? extends PlayList> c) {
                try{
                    while (c.next()) {
                        if (c.wasAdded()){
                            for (PlayList playList : c.getAddedSubList()){
                                if (playList.getId() == 0) //background
                                    continue;

                                FilteredList<Tab> filteredData = new FilteredList<>(playerTabPane.getTabs(), p -> true);
                                filteredData.setPredicate(tab -> tab.getText().equals(playList.getName()));
                                if (filteredData.size() == 0){
                                    addNewTab(playList);
                                }
                            }
                        }

                        if (c.wasRemoved()){
                            for (PlayList playList : c.getRemoved()){
                                if (playList.getId() == 0) //background
                                    continue;
                                FilteredList<Tab> filteredData = new FilteredList<>(playerTabPane.getTabs(), p -> true);
                                filteredData.setPredicate(tab -> tab.getText().equals(playList.getName()));
                                playerTabPane.getTabs().removeAll(filteredData);
                            }
                        }
                    }
                }catch (Exception e){log.error(e.getMessage(), e);}
            }
        });

        //tabs graphic
        playerTabPane.getTabs().stream().filter(tab -> tab.getGraphic() != null).forEach(tab -> {
            tab.getGraphic().setVisible(false);
        });
        if (mMaxPlayer.getPlayingPlaylist().getValue() == null || mMaxPlayer.getPlayingPlaylist().getValue().getId() == 0)
            playerTabPane.getTabs().get(2).getGraphic().setVisible(true);
        else{
            FilteredList<Tab> filteredData = new FilteredList<>(playerTabPane.getTabs(), p -> true);
            filteredData.setPredicate(tab -> tab.getText().equals(mMaxPlayer.getPlayingPlaylist().getValue().getName()));
            if (filteredData.size() > 0) filteredData.get(0).getGraphic().setVisible(true);
        }

        mMaxPlayer.getPlayingPlaylist().addListener((observable, oldValue, newValue) -> {
            playerTabPane.getTabs().stream().filter(tab -> tab.getGraphic() != null).forEach(tab -> {
                tab.getGraphic().setVisible(false);
            });
            if (newValue.getId() == 0)
                playerTabPane.getTabs().get(2).getGraphic().setVisible(true);
            else{
                FilteredList<Tab> filteredData = new FilteredList<>(playerTabPane.getTabs(), p -> true);
                filteredData.setPredicate(tab -> tab.getText().equals(newValue.getName()));
                if (filteredData.size() > 0) filteredData.get(0).getGraphic().setVisible(true);
            }
        });
    }

    private void addNewTab(PlayList playList){
        Platform.runLater(() -> {
            for (Tab tab : playerTabPane.getTabs()){
                if (tab.getUserData() != null && ((PlayList)tab.getUserData()).getId() == playList.getId())
                    return; //already added
            }

            final Tab tab = new Tab(playList.getName());
            try {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(TrackListTabController.class.getResource("TrackListTab.fxml"));
                AnchorPane content = loader.load();
                TrackListTabController tabController = loader.getController();
                content.setUserData(tabController);

                content.setPrefWidth(Region.USE_COMPUTED_SIZE);
                content.setPrefHeight(Region.USE_COMPUTED_SIZE);

                SVGPath svg = new SVGPath();
                svg.setContent("M8 5v14l11-7z");
                svg.setFill(Color.BLUE);
                tab.setGraphic(svg);
                tab.getGraphic().setVisible(false);

                tab.setContent(content);
                tab.setUserData(playList);
                tabController.setPlayListData(mMaxPlayer, playList, tab);
                playerTabPane.getTabs().add(tab);
            } catch (IOException e) {
                log.error("TrackListTab.fxml", e);
            }
        });
    }

    private void checkEmptyBackgroundPlayList(){
        Settings settings = ConfigFactory.create(Settings.class);
        File backgroundPlaylist = new File(settings.BACKGROUNDS_DIR() + File.separator + MaxPlayer.BACKGROUNDS_PLAYLIST_FILE_NAME);
        ObservableList<PlaylistItem> backgroundItems = mMaxPlayer.getPlaylistOnId(0).getTrackPlaylistItems();

        boolean needFillPlayList = backgroundPlaylist.exists() &&
                                   backgroundPlaylist.length() == 0 &&
                                   backgroundPlaylist.getParentFile().listFiles().length > 0 &&
                                   backgroundItems.size() == 0;

        if (needFillPlayList){
            Optional<ButtonType> result = Utils.getStylingAlert(Alert.AlertType.CONFIRMATION,
                    "Заполнить фоновый плейлист?", "Заполнить фоновый плейлист по умолчанию?",
                    "Будут добавлены все MP3 файлы из \n" + backgroundPlaylist.getParentFile().getAbsolutePath()).showAndWait();

            if (result.get() == ButtonType.OK){
                StringBuilder sb = new StringBuilder();
                for (File f : backgroundPlaylist.getParentFile().listFiles()) {
                    if (Utils.isMp3(f)) sb.append(f.getName()).append("\n");
                }

                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                                                new FileOutputStream(backgroundPlaylist.getAbsolutePath()), "utf-8"))) {
                    bw.write(sb.toString());
                } catch (IOException exxx) {
                    String msg = "Не могу перезаписать файл " + backgroundPlaylist.getAbsolutePath();
                    Utils.getStylingAlert(Alert.AlertType.ERROR, "Внимание", msg, exxx.getMessage()).show();
                    log.error(msg, exxx);
                    return;
                }
                mMaxPlayer.reloadPlayList(0l); //reload background playlist
            }
        }
    }

    @FXML
    private void handlePlayBackgroundTrack() {
        try {
            mMaxPlayer.playTrack(0, backgroundTable.getFocusModel().getFocusedIndex());
        } catch (BasicPlayerException e) {
            log.error(e.getMessage(), e);
        }
    }

    @FXML
    private void handlePlayerStop() {
        try {
            mMaxPlayer.stopPlayer();
        } catch (BasicPlayerException e) {
            log.error(e.getMessage(), e);
        }
    }

}
