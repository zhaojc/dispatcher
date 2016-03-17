package simpleci.dispatcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class JsonUtils {

    public static Date tsToDate(String timestamp) {
        return new Date(Long.valueOf(timestamp));
    }

    public static List<Integer> jsonArrayToIntList(JsonArray array) {
        List<Integer> list = new ArrayList<>(array.size());
        for(JsonElement element : array) {
            list.add(element.getAsInt());
        }
        return list;
    }
}
