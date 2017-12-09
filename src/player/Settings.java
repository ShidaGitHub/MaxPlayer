package player;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Reloadable;

@Config.Sources("file:Settings.properties")
@Config.HotReload(1)
public interface Settings extends Config, Reloadable {
    public final static String SETTINGS_FILE_NAME = "Settings.properties";

    public final static String PLAYER_ID_PROPERTY_NAME = "PLAYER_ID";
    public final static String SERVER_IP_PROPERTY_NAME = "SERVER_IP";
    public final static String SERVER_PORT_PROPERTY_NAME = "SERVER_PORT";
    public final static String VOLUME_TRACK_PLAYLIST_PROPERTY_NAME = "VOLUME_TRACK_PLAYLIST";
    public final static String VOLUME_BACKGROUND_PLAYLIST_PROPERTY_NAME = "VOLUME_BACKGROUND_PLAYLIST_LIST";
    public final static String TRACKS_DIR_PROPERTY_NAME = "TRACKS_DIR";
    public final static String BACKGROUNDS_DIR_PROPERTY_NAME = "BACKGROUNDS_DIR";
    public final static String TEMP_DIR_PROPERTY_NAME = "TEMP_DIR";
    public final static String CHECK_UPDATE_SCHEDULE_PROPERTY_NAME = "CHECK_UPDATE_SCHEDULE";

    @DefaultValue("0")
    long PLAYER_ID();

    @DefaultValue("localhost")
    String SERVER_IP();

    @DefaultValue("3486")
    String SERVER_PORT();

    @DefaultValue("100")
    int VOLUME_TRACK_PLAYLIST();
    @DefaultValue("100")
    int VOLUME_BACKGROUND_PLAYLIST();

    String TRACKS_DIR();
    String BACKGROUNDS_DIR();
    String TEMP_DIR();

    @DefaultValue("0 * * * * ?")
    String CHECK_UPDATE_SCHEDULE();
}
