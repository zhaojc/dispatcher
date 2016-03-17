package simpleci.dispatcher.message;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class BuildRequestMessage {
    public final int buildId;

    public BuildRequestMessage(JsonObject message) {
       buildId = message.get("build_id").getAsInt();
    }
}
