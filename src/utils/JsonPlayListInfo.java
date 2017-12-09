package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.LinkedList;
import java.util.List;

public class JsonPlayListInfo {
    public JsonPlayListInfo(){
        tracks = new LinkedList<>();
    }

    @SerializedName("id")
    public long id;

    @SerializedName("name")
    public String name;

    @SerializedName("cronTab")
    public String cronTab;

    @SerializedName("startTime")
    public long startTime;

    @SerializedName("endTime")
    public long endTime;

    @SerializedName("tracks")
    public List<String> tracks;

    public String getJsonString(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
