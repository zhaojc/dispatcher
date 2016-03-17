package simpleci.dispatcher.message;

import com.google.gson.JsonObject;

import java.util.Date;
import java.util.Map;

import static simpleci.dispatcher.JsonUtils.tsToDate;

public class JobStoppedMessage {
    public final int jobId;
    public final String workerId;
    public final Date stoppedAt;
    public final String jobStatus;


    public JobStoppedMessage(JsonObject message) {
        jobId = message.get("job_id").getAsInt();
        stoppedAt = tsToDate(message.get("stopped_at").getAsString());
        jobStatus = message.get("job_status").getAsString();
        workerId = message.get("worker_id").getAsString();
    }
}
