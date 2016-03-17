package simpleci.dispatcher.message;


import com.google.gson.JsonObject;

import java.util.Date;
import java.util.Map;

import static simpleci.dispatcher.JsonUtils.tsToDate;

public class JobStartedMessage {
    public final int jobId;
    public final Date startedAt;
    public final String workerId;

    public JobStartedMessage(JsonObject message) {
        jobId = message.get("job_id").getAsInt();
        startedAt = tsToDate(message.get("started_at").getAsString());
        workerId = message.get("worker_id").getAsString();
    }
}
