package skin_jfx;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import org.slf4j.LoggerFactory;
import player.MaxPlayer;
import player.PlayList;
import player.PlaylistItem;
import utils.Utils;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class TrackListTabController implements Initializable {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(PlaylistItem.class);
    private MaxPlayer mMaxPlayer;
    private PlayList mPlayList;

    @FXML
    private TableView<PlaylistItem> tracksTable;
    @FXML
    private TableColumn colTrackRowNumber;
    @FXML
    private TableColumn<PlaylistItem, String> colTrackName, colTrackTotal;
    @FXML
    private TableColumn<PlaylistItem, File> colTrackFile;
    @FXML
    private Label trackPlayListInfo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Utils.initNumberRowColumn(colTrackRowNumber);
        Utils.initNamePlayListItemColumn(colTrackName);
        Utils.initDurationPlayListItemColumn(colTrackTotal);
        Utils.initFileColumn(colTrackFile);

    }

    public void setPlayListData(MaxPlayer maxPlayer, PlayList playList, Tab parent){
        mMaxPlayer = maxPlayer;
        mPlayList = playList;
        tracksTable.setItems(mPlayList.getTrackPlaylistItems());
    }

    @FXML
    private void handleTrackAddRow() {
        mPlayList.getTrackPlaylistItems().add(new PlaylistItem(null));
    }

    @FXML
    private void handleTrackDelRow() {
        if (tracksTable.getSelectionModel().getSelectedItem() == null && mPlayList.getTrackPlaylistItems().size() > 0)
            mPlayList.getTrackPlaylistItems().remove(mPlayList.getTrackPlaylistItems().size() - 1);
        else
            mPlayList.getTrackPlaylistItems().remove(tracksTable.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleTrackAddFromFolder() {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(Utils.lastDir);
        File file = directoryChooser.showDialog(null);

        if (file != null) {
            Utils.lastDir = file;
            for (File f : file.listFiles()) {
                if (Utils.isMp3(f)) {
                    mPlayList.getTrackPlaylistItems().add(new PlaylistItem(f));
                }
            }
        }
    }

    @FXML
    private void handleTrackUpRow() {
        if (tracksTable.getSelectionModel().getSelectedItem() != null){
            PlaylistItem selItem = tracksTable.getSelectionModel().getSelectedItem();
            int selItemInd = tracksTable.getSelectionModel().getFocusedIndex();

            mPlayList.getTrackPlaylistItems().add(Math.max(0, selItemInd - 1), selItem);
            mPlayList.getTrackPlaylistItems().remove(selItemInd + 1);
            tracksTable.getSelectionModel().select(selItem);
        }
    }

    @FXML
    private void handleTrackDownRow() {
        if (tracksTable.getSelectionModel().getSelectedItem() != null){
            PlaylistItem selItem = tracksTable.getSelectionModel().getSelectedItem();
            int selItemInd = tracksTable.getSelectionModel().getFocusedIndex();

            mPlayList.getTrackPlaylistItems().add(Math.min(mPlayList.getTrackPlaylistItems().size(), selItemInd + 2), selItem);
            mPlayList.getTrackPlaylistItems().remove(selItemInd);
            tracksTable.getSelectionModel().select(selItem);
        }
    }

    @FXML
    private void handlePlayTrack() {
        try {
            mMaxPlayer.playTrack(mPlayList.getId(), tracksTable.getFocusModel().getFocusedIndex());
        } catch (BasicPlayerException e) {
            log.error(e.getMessage(), e);
        }
    }
}
