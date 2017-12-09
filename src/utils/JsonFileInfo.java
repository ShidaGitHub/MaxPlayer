package utils;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class JsonFileInfo {
    @SerializedName("FILE_NAME")
    public String file_name;

    @SerializedName("START_BYTE")
    public int start_byte;

    public String getJsonString(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
