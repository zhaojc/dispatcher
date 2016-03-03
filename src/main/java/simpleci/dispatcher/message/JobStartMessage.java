package simpleci.dispatcher.message;


import java.util.Date;
import java.util.Map;

public class JobStartMessage {
    public int jobId;
    public Date startedAt;

    public static JobStartMessage fromJson(Map logMessage) {
        JobStartMessage message = new JobStartMessage();
        message.jobId = new Double((double) logMessage.get("job_id")).intValue();
        message.startedAt = new Date(Long.valueOf((String) logMessage.get("started_at")));
        return message;
    }
}
