package player.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import javafx.collections.transformation.FilteredList;
import org.aeonbits.owner.ConfigFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import player.MaxPlayer;
import player.PlayList;
import player.PlaylistItem;
import player.Settings;
import utils.JsonPlayListCollection;
import utils.JsonPlayListInfo;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PlayListCollectionSyncJob implements Job {
    private static Logger log = LoggerFactory.getLogger(PlayListCollectionSyncJob.class);
    public static final String KEY_MAX_PLAYER = "KEY_MAX_PLAYER";
    public static final String KEY_MAX_CLIENT = "KEY_MAX_CLIENT";
    public static final String KEY_MESSAGE = "KEY_MESSAGE";

    public PlayListCollectionSyncJob(){}

    public void execute(JobExecutionContext context) throws JobExecutionException {
        MaxPlayer maxPlayer = (MaxPlayer) context.getJobDetail().getJobDataMap().get(KEY_MAX_PLAYER);
        MaxClient maxClient = (MaxClient) context.getJobDetail().getJobDataMap().get(KEY_MAX_CLIENT);
        String message = (String) context.getJobDetail().getJobDataMap().get(KEY_MESSAGE);
        Settings settings = ConfigFactory.create(Settings.class);
        Gson gson = new Gson();
        JsonPlayListCollection playLists =  gson.fromJson(message, JsonPlayListCollection.class);

        LinkedList<String> needDownload = new LinkedList<>();

        for (JsonPlayListInfo playListInfo : playLists.playlistCollection){
            FilteredList<PlayList> filteredData = new FilteredList<>(maxPlayer.getPlaylistCollection(), p -> true);
            filteredData.setPredicate(playList -> playList.getId() == playListInfo.id);

            PlayList playList;
            if (filteredData.size() == 0) { //create new playlist
                playList = new PlayList(playListInfo.id, playListInfo.name);
                playList.setCronTab(playListInfo.cronTab).setStartTime(playListInfo.startTime).setEndTime(playListInfo.endTime);
                maxPlayer.getPlaylistCollection().add(playList);
            }else { //get old list
                playList = filteredData.get(0);
                if (!playList.getCronTab().equalsIgnoreCase(playListInfo.cronTab))
                    playList.setCronTab(playListInfo.cronTab);
                if (playList.getStartTime() != playListInfo.startTime)
                    playList.setStartTime(playListInfo.startTime);
                if (playList.getEndTime() != playListInfo.endTime)
                    playList.setEndTime(playListInfo.endTime);
            }
            playList.getTrackPlaylistItems().clear();
            String workDir = playList.getId() == 0 ? settings.BACKGROUNDS_DIR() : settings.TRACKS_DIR();
            for (String trackName : playListInfo.tracks){
                playList.getTrackPlaylistItems().add(new PlaylistItem(new File(workDir + File.separator + trackName)));
            }
            try {
                playList.savePlaylistAsM3UFile(workDir);
                maxPlayer.reloadPlayList(playList.getId());
            } catch (IOException e) {
                log.error("Can't save m3u file to " + workDir, e);
            }
            playList.getTrackPlaylistItems().stream().
                    filter(pli -> !pli.getFile().exists() && !needDownload.contains(pli.getFile().getName())).
                    forEach(pli -> needDownload.add(pli.getFile().getName()));
        }

        //delete wrong playlist's
        LinkedList<PlayList> delList = new LinkedList<>();
        for (PlayList pl : maxPlayer.getPlaylistCollection()){
            boolean find = false;
            for (JsonPlayListInfo playListInfo : playLists.playlistCollection){
                if (playListInfo.id == pl.getId())
                    find = true;
            }
            if (!find) delList.add(pl);
        }
        maxPlayer.getPlaylistCollection().removeAll(delList);

        //sync files
        for (String ndn : needDownload)
            maxClient.sendCommandToServerOnDownloadFile(ndn);
    }
}
