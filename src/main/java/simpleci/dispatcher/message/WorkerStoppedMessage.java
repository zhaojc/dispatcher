package simpleci.dispatcher.message;

import com.google.gson.JsonObject;

import java.util.Date;
import java.util.Map;

import static simpleci.dispatcher.JsonUtils.tsToDate;

public class WorkerStoppedMessage {
    public final String workerId;
    public final String workerHostName;
    public final String workerType;
    public final Date stoppedAt;
    public final int executedTime;

    public WorkerStoppedMessage(JsonObject message) {
        workerId = message.get("worker_id").getAsString();
        workerHostName = message.get("worker_hostname").getAsString();
        workerType = message.get("worker_type").getAsString();
        stoppedAt = tsToDate(message.get("stopped_at").getAsString());
        executedTime = message.get("executed_time").getAsInt();
    }


}
