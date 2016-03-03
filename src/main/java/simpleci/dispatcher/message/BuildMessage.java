package simpleci.dispatcher.message;

import java.util.HashMap;
import java.util.Map;

public class BuildMessage {
    public int buildId;

    public static BuildMessage fromJson(Map logMessage) {
        BuildMessage build = new BuildMessage();
        build.buildId = new Double((double) logMessage.get("build_id")).intValue();

        return build;
    }
}
