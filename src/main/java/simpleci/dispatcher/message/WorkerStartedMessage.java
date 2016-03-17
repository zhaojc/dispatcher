package simpleci.dispatcher.message;

import com.google.gson.JsonObject;

import java.util.Date;
import java.util.Map;

import static simpleci.dispatcher.JsonUtils.tsToDate;

public class WorkerStartedMessage {
    public final String workerId;
    public final String workerType;
    public final Date startedAt;

    public WorkerStartedMessage(JsonObject message) {
        workerId = message.get("worker_id").getAsString();
        workerType = message.get("worker_type").getAsString();
        startedAt = tsToDate(message.get("started_at").getAsString());
    }
}
