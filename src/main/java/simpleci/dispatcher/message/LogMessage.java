package simpleci.dispatcher.message;

import com.google.gson.JsonElement;

public interface LogMessage {
    JsonElement toJson();
}
