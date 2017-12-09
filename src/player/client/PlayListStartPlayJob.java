package player.client;

import javazoom.jlgui.basicplayer.BasicPlayerException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import player.MaxPlayer;

public class PlayListStartPlayJob implements Job {
    private static Logger log = LoggerFactory.getLogger(PlayListStartPlayJob.class);
    public static final String KEY_MAX_PLAYER = "KEY_MAX_PLAYER";
    public static final String KEY_PLAYLIST_ID = "KEY_PLAYLIST_ID";

    public PlayListStartPlayJob(){}

    public void execute(JobExecutionContext context) throws JobExecutionException {
        MaxPlayer maxPlayer = (MaxPlayer) context.getJobDetail().getJobDataMap().get(KEY_MAX_PLAYER);
        long playlistId = (long) context.getJobDetail().getJobDataMap().get(KEY_PLAYLIST_ID);

        try {
            maxPlayer.playTrack(playlistId, 0);
        } catch (BasicPlayerException e) {
            log.error(e.getMessage(), e);
        }
    }
}
