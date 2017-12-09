package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.LinkedList;
import java.util.List;

public class JsonPlayListCollection {
    @SerializedName("PLAYLIST_COLLECTION")
    public List<JsonPlayListInfo> playlistCollection;

    public JsonPlayListCollection(){
        playlistCollection = new LinkedList<>();
    }

    public String getJsonString(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
