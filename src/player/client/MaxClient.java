package player.client;

import com.google.gson.Gson;
import javafx.application.Platform;
import org.aeonbits.owner.ConfigFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import player.MaxPlayer;
import player.PlayList;
import player.Settings;
import syncUtils.SyncPacket;
import utils.ByteBufferBackedInputStream;
import utils.JsonFileInfo;
import utils.JsonPlayListInfo;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.rmi.server.UID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.impl.matchers.KeyMatcher.keyEquals;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

public class MaxClient {
    private static Logger log = LoggerFactory.getLogger(MaxClient.class);

    public static final String COMMAND_PLAYLIST_COLLECTION_GET = "|*PLAYLIST_COLLECTION_GET";
    public static final String COMMAND_PLAYLIST_UPDATE = "|*PLAYLIST_UPDATE";
    public static final String COMMAND_PLAYLIST_DELETE = "|*PLAYLIST_DELETE";
    public static final String COMMAND_FILE_GET = "|*FILE_GET ";

    private static final String TRIGGER_START_NOW_NAME = "TRIGGER_START_NOW";
    private static final String CONNECT_JOB_GROUP_NAME = "CONNECT_JOB_GROUP";
    private static final String CONNECT_JOB_NAME = "CONNECT_JOB";
    private static final String TRIGGER_CONNECT_JOB_NAME = "TRIGGER_CONNECT_JOB";

    private static final String PLAYLIST_COLLECTION_JOB_NAME = "PLAYLIST_COLLECTION_JOB";
    private static final String PLAYLIST_UPDATE_JOB_NAME = "PLAYLIST_UPDATE_JOB";
    private static final String PLAYLIST_DELETE_JOB_NAME = "PLAYLIST_DELETE_JOB";
    private static final String PLAYLIST_SYNC_JOB_GROUP_NAME = "PLAYLIST_SYNC_JOB_GROUP";

    private static final String PLAYLIST_PLAY_JOB_NAME = "PLAYLIST_PLAY_JOB";
    private static final String PLAYLIST_PLAY_JOB_GROUP_NAME = "PLAYLIST_PLAY_JOB_GROUP";
    private static final String TRIGGER_PLAYLIST_PLAY_JOB_NAME = "TRIGGER_PLAYLIST_PLAY_JOB_NAME";

    private WebSocketClient mWebSocketClient;
    private URI mServerUri;
    private MaxPlayer mMaxPlayer;
    private Scheduler mScheduler;

    public MaxClient(URI serverUri, MaxPlayer maxPlayer) throws SchedulerException {
        mServerUri = serverUri;
        createNewWebSocketClient();
        mMaxPlayer = maxPlayer;
        SchedulerFactory sf = new StdSchedulerFactory();
        mScheduler = sf.getScheduler();


        new JobPlaylistSyncListener().jobWasExecuted(null, null); //update playlist schedule

        Trigger startNowTrigger = newTrigger().withIdentity(TRIGGER_START_NOW_NAME).startNow().build();

        CronTrigger connectTrigger = newTrigger().withIdentity(TRIGGER_CONNECT_JOB_NAME, CONNECT_JOB_GROUP_NAME).
                                                  withSchedule(cronSchedule("0 * * * * ?")).build();

        JobDetail connectJob = newJob(ConnectJob.class).withIdentity(CONNECT_JOB_NAME, CONNECT_JOB_GROUP_NAME).build();
        connectJob.getJobDataMap().put(ConnectJob.KEY_MAX_CLIENT, this);

        HashSet<Trigger> triggerSet = new HashSet<>();
        triggerSet.add(connectTrigger);

        Date dateStartNow =  startNowTrigger.getFinalFireTime();
        Date dateCron =  connectTrigger.getFireTimeAfter(dateStartNow);
        if (dateCron.getTime() - dateStartNow.getTime() > 4000)
            triggerSet.add(startNowTrigger);

        Map<JobDetail, Set<? extends Trigger>> connectJobMap = new HashMap<>();
        connectJobMap.put(connectJob, triggerSet);

        try {
            mScheduler.scheduleJobs(connectJobMap, true);
        } catch (SchedulerException e) {
            log.error("Can't start connect job", e);
        }
        mScheduler.start();
    }

    public WebSocketClient createNewWebSocketClient(){
        mWebSocketClient = new WebSocketClient(mServerUri, new Draft_17()) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                log.debug("mWebSocketClient onOpen ", serverHandshake);
            }

            @Override
            public void onMessage(String message) {
                log.debug("mWebSocketClient onMessage " + message);

                if (message.startsWith("{\n  \"PLAYLIST_COLLECTION\":")){
                    JobDetail playListsSyncJob = newJob(PlayListCollectionSyncJob.class).
                            withIdentity(PLAYLIST_COLLECTION_JOB_NAME, PLAYLIST_SYNC_JOB_GROUP_NAME).build();
                    playListsSyncJob.getJobDataMap().put(PlayListCollectionSyncJob.KEY_MAX_PLAYER, mMaxPlayer);
                    playListsSyncJob.getJobDataMap().put(PlayListCollectionSyncJob.KEY_MAX_CLIENT, MaxClient.this);
                    playListsSyncJob.getJobDataMap().put(PlayListCollectionSyncJob.KEY_MESSAGE, message);

                    Trigger trigger = newTrigger().withIdentity(TRIGGER_START_NOW_NAME + "_" + new UID(), PLAYLIST_SYNC_JOB_GROUP_NAME).
                                                                                forJob(playListsSyncJob).startNow().build();

                    try {
                        if (mScheduler.checkExists(playListsSyncJob.getKey())){
                            mScheduler.deleteJob(playListsSyncJob.getKey());
                            mScheduler.getListenerManager().removeJobListener(JobPlaylistSyncListener.JOB_PLAYLIST_SYNC_LISTENER_NAME);
                        }
                        mScheduler.scheduleJob(playListsSyncJob, trigger);
                        mScheduler.getListenerManager().addJobListener(
                                new JobPlaylistSyncListener(), keyEquals(jobKey(PLAYLIST_COLLECTION_JOB_NAME, PLAYLIST_SYNC_JOB_GROUP_NAME)));

                    } catch (SchedulerException e) {
                        log.error("Can't start PLAYLIST_COLLECTION sync job", e);
                    }
                }

                if (message.startsWith(COMMAND_PLAYLIST_UPDATE)){
                    String jobName = PLAYLIST_UPDATE_JOB_NAME + "_" + new UID();
                    JobDetail playListUpdateJob = newJob(PlayListUpdateJob.class).
                            withIdentity(jobName, PLAYLIST_SYNC_JOB_GROUP_NAME).build();
                    playListUpdateJob.getJobDataMap().put(PlayListCollectionSyncJob.KEY_MAX_PLAYER, mMaxPlayer);
                    playListUpdateJob.getJobDataMap().put(PlayListCollectionSyncJob.KEY_MAX_CLIENT, MaxClient.this);
                    playListUpdateJob.getJobDataMap().put(PlayListCollectionSyncJob.KEY_MESSAGE, message);

                    Trigger trigger = newTrigger().withIdentity(TRIGGER_START_NOW_NAME + "_" + new UID(), PLAYLIST_SYNC_JOB_GROUP_NAME).
                                                                                            forJob(playListUpdateJob).startNow().build();

                    try {
                        mScheduler.scheduleJob(playListUpdateJob, trigger);
                        mScheduler.getListenerManager().addJobListener(
                                new JobPlaylistUpdateListener(), keyEquals(jobKey(jobName, PLAYLIST_SYNC_JOB_GROUP_NAME)));

                    } catch (SchedulerException e) {
                        log.error("Can't start PLAYLIST  update job", e);
                    }
                }
            }

            @Override
            public void onMessage(ByteBuffer byteBuffer){
                try (ObjectInputStream is = new ObjectInputStream(new ByteBufferBackedInputStream(byteBuffer))) {
                    SyncPacket syncPacket = SyncPacket.class.cast(is.readObject());
                    log.debug("onMessage(ByteBuffer byteBuffer) " + syncPacket.toString());

                    Settings settings = ConfigFactory.create(Settings.class);
                    File downloadTPMFile = new File(settings.TEMP_DIR() +  File.separator + syncPacket.getFileName() + ".tmp");

                    RandomAccessFile raf = new RandomAccessFile(downloadTPMFile, "rw");
                    FileChannel fileChannel = raf.getChannel();
                    MappedByteBuffer mbb = fileChannel.
                            map(FileChannel.MapMode.READ_WRITE, syncPacket.getSliceNumber() * SyncPacket.SLICE_SIZE, syncPacket.getData().length);
                    mbb.put(syncPacket.getData());
                    fileChannel.close(); fileChannel = null;
                    raf.close(); raf = null;
                    System.gc();

                    if (downloadTPMFile.length() > syncPacket.getTotalLength()){ //screwed up
                        File bait =new File(downloadTPMFile.getParentFile() + File.separator + "4715154.bait");//http://bugs.java.com/view_bug.do?bug_id=4715154
                        raf = new RandomAccessFile(bait, "rw");
                        fileChannel = raf.getChannel();
                        mbb = fileChannel.map(FileChannel.MapMode.READ_WRITE, 1 * SyncPacket.SLICE_SIZE, syncPacket.getData().length);
                        mbb.put(syncPacket.getData());
                        fileChannel.close(); fileChannel = null;
                        raf.close(); raf = null;
                        bait.delete();
                        System.gc();
                        downloadTPMFile.delete(); //http://bugs.java.com/view_bug.do?bug_id=4715154
                        sendCommandToServerOnDownloadFile(syncPacket.getFileName());
                        return;
                    }

                    if (downloadTPMFile.length() == syncPacket.getTotalLength()){ //end download
                        downloadTPMFile.deleteOnExit();
                        //move to work dir
                        File downloadedFile = new File(downloadTPMFile.getAbsolutePath().substring(0, downloadTPMFile.getAbsolutePath().length() - 4));
                        LinkedList<Long> plc = mMaxPlayer.fileInPlaylist(downloadedFile);
                        if (plc.contains(0l)){
                            Files.copy(Paths.get(downloadTPMFile.getAbsolutePath()),
                                    Paths.get(settings.BACKGROUNDS_DIR() + File.separator + downloadedFile.getName()),
                                    StandardCopyOption.REPLACE_EXISTING);
                            mMaxPlayer.reloadPlayList(0l);
                            plc.remove(0l);
                        }
                        if (plc.size() > 0){
                            Files.copy(Paths.get(downloadTPMFile.getAbsolutePath()),
                                    Paths.get(settings.TRACKS_DIR() + File.separator + downloadedFile.getName()),
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                        for (long id : plc)
                            mMaxPlayer.reloadPlayList(id);

                        File bait =new File(downloadTPMFile.getParentFile() + File.separator + "4715154.bait");//http://bugs.java.com/view_bug.do?bug_id=4715154
                        raf = new RandomAccessFile(bait, "rw");
                        fileChannel = raf.getChannel();
                        mbb = fileChannel.map(FileChannel.MapMode.READ_WRITE, 1 * SyncPacket.SLICE_SIZE, syncPacket.getData().length);
                        mbb.put(syncPacket.getData());
                        fileChannel.close(); fileChannel = null;
                        raf.close(); raf = null;
                        bait.delete();
                        System.gc();
                        downloadTPMFile.delete(); //http://bugs.java.com/view_bug.do?bug_id=4715154
                        log.info("File " + downloadedFile.getAbsolutePath() + " downloaded");
                    }
                } catch (ClassNotFoundException| IOException e) {
                    log.error("onMessage(ByteBuffer byteBuffer) ", e);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                log.info("onClose  code " + code + " reason: " + reason + " remote: " + remote);
            }

            @Override
            public void onError(Exception e) {
                log.error("onError", e);
            }
        };
        return mWebSocketClient;
    }

    public WebSocketClient getWebSocketClient() {
        return mWebSocketClient;
    }

    /**
     *
     * @param fileName
     */
    public void sendCommandToServerOnDownloadFile(String fileName){
        Settings settings = ConfigFactory.create(Settings.class);

        JsonFileInfo fileInfo = new JsonFileInfo();
        fileInfo.file_name = fileName;
        File tempFile = new File(settings.TEMP_DIR() + File.separator + fileName + ".tmp");
        fileInfo.start_byte = !tempFile.exists() ? 0 : (int) tempFile.length();

        mWebSocketClient.send(MaxClient.COMMAND_FILE_GET + fileInfo.getJsonString());
        log.debug(MaxClient.COMMAND_FILE_GET + fileInfo.getJsonString());
    }

    public URI getServerUri() {
        return mServerUri;
    }

    public void setServerUri(URI serverUri) {
        this.mServerUri = serverUri;
    }

    public Scheduler getScheduler() {
        return mScheduler;
    }

    public void syncPlayListTracks(PlayList playList){

    }

    private class JobPlaylistSyncListener implements JobListener {
        public static final String JOB_PLAYLIST_SYNC_LISTENER_NAME = "JOB_PLAYLIST_SYNC_LISTENER_NAME";

        @Override
        public String getName() {
            return JOB_PLAYLIST_SYNC_LISTENER_NAME;
        }

        @Override
        public void jobToBeExecuted(JobExecutionContext jobExecutionContext) {

        }

        @Override
        public void jobExecutionVetoed(JobExecutionContext jobExecutionContext) {

        }

        @Override
        public void jobWasExecuted(JobExecutionContext jobExecutionContext, JobExecutionException e) {
            log.debug(PLAYLIST_COLLECTION_JOB_NAME + " was executed ");
            try {
                for (JobKey jobKey : mScheduler.getJobKeys(GroupMatcher.jobGroupEquals(PLAYLIST_PLAY_JOB_GROUP_NAME))) {
                    mScheduler.deleteJob(jobKey);
                }
            } catch (SchedulerException e1) {
                log.error(e1.getMessage(), e1);
            }

            for (PlayList playList : mMaxPlayer.getPlaylistCollection()){
                if (playList.getId() == 0l) continue; //background

                JobDetail playlistStartPlayJob = newJob(PlayListStartPlayJob.class).
                        withIdentity(PLAYLIST_PLAY_JOB_NAME + "_" + playList.getId(), PLAYLIST_PLAY_JOB_GROUP_NAME).build();
                playlistStartPlayJob.getJobDataMap().put(PlayListStartPlayJob.KEY_MAX_PLAYER, mMaxPlayer);
                playlistStartPlayJob.getJobDataMap().put(PlayListStartPlayJob.KEY_PLAYLIST_ID, playList.getId());

                if (!playList.getCronTab().isEmpty()){
                    CronTrigger playTrigger = newTrigger().
                            withIdentity(TRIGGER_PLAYLIST_PLAY_JOB_NAME + "_" + playList.getId(), PLAYLIST_PLAY_JOB_GROUP_NAME).
                            withSchedule(cronSchedule(playList.getCronTab())).build();

                    try {
                        mScheduler.scheduleJob(playlistStartPlayJob, playTrigger);
                    } catch (SchedulerException e1) {
                        log.error(e1.getMessage(), e1);
                    }
                }else if (playList.getStartTime() > 0 || playList.getEndTime() > 0){
                    LocalDateTime ldt = LocalDateTime.of(LocalDate.now(), LocalTime.ofSecondOfDay(playList.getStartTime()));
                    Date start = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
                    SimpleTrigger playTrigger = newTrigger().
                            withIdentity(TRIGGER_PLAYLIST_PLAY_JOB_NAME + "_" + playList.getId(), PLAYLIST_PLAY_JOB_GROUP_NAME).
                            startAt(start).
                            withSchedule(simpleSchedule().
                                    withIntervalInHours(24).
                                    withMisfireHandlingInstructionNextWithExistingCount().
                                    repeatForever()).
                            build();

                    try {
                        mScheduler.scheduleJob(playlistStartPlayJob, playTrigger);
                    } catch (SchedulerException e1) {
                        log.error(e1.getMessage(), e1);
                    }
                }
            }

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    mMaxPlayer.reloadTrackPlayListCollection();
                }
            });
        }
    }

    private class JobPlaylistUpdateListener implements JobListener {
        public static final String JOB_PLAYLIST_SYNC_LISTENER_NAME = "JOB_PLAYLIST_UPDATE_LISTENER_NAME";

        @Override
        public String getName() {
            return JOB_PLAYLIST_SYNC_LISTENER_NAME;
        }

        @Override
        public void jobToBeExecuted(JobExecutionContext jobExecutionContext) {

        }

        @Override
        public void jobExecutionVetoed(JobExecutionContext jobExecutionContext) {

        }

        @Override
        public void jobWasExecuted(JobExecutionContext jobExecutionContext, JobExecutionException e) {
            //find updated playlist schedule
            String message = (String) jobExecutionContext.getJobDetail().getJobDataMap().get(PlayListUpdateJob.KEY_MESSAGE);
            if (message != null){
                JsonPlayListInfo playListInfo =  new Gson().fromJson(message.substring(MaxClient.COMMAND_PLAYLIST_UPDATE.length() + 1), JsonPlayListInfo.class);
                if (playListInfo != null){
                    if (playListInfo.id == 0) return; //background

                    //reschedule
                    Trigger playTrigger = null;
                    if (!playListInfo.cronTab.isEmpty()){
                        playTrigger = newTrigger().
                                withIdentity(TRIGGER_PLAYLIST_PLAY_JOB_NAME + "_" + playListInfo.id, PLAYLIST_PLAY_JOB_GROUP_NAME).
                                withSchedule(cronSchedule(playListInfo.cronTab)).build();
                    }else if (playListInfo.startTime > 0 || playListInfo.endTime > 0){
                        LocalDateTime ldt = LocalDateTime.of(LocalDate.now(), LocalTime.ofSecondOfDay(playListInfo.startTime));
                        Date start = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
                        playTrigger = newTrigger().
                                withIdentity(TRIGGER_PLAYLIST_PLAY_JOB_NAME + "_" + playListInfo.id, PLAYLIST_PLAY_JOB_GROUP_NAME).
                                startAt(start).
                                withSchedule(simpleSchedule().
                                        withIntervalInHours(24).
                                        withMisfireHandlingInstructionNextWithExistingCount().
                                        repeatForever()).build();
                    }
                    if (playTrigger == null) return;
                    try {
                        mScheduler.rescheduleJob(triggerKey(TRIGGER_PLAYLIST_PLAY_JOB_NAME + "_" + playListInfo.id, PLAYLIST_PLAY_JOB_GROUP_NAME), playTrigger);
                        log.debug("rescheduleJob " + TRIGGER_PLAYLIST_PLAY_JOB_NAME + "_" + playListInfo.id);
                    } catch (SchedulerException e1) { log.error("Can't start play job", e); }
                }
            }
        }
    }

}
