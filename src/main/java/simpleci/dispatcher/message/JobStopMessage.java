package simpleci.dispatcher.message;

import java.util.Date;
import java.util.Map;

public class JobStopMessage {
    public int jobId;
    public Date endedAt;
    public String jobStatus;


    public static JobStopMessage fromJson(Map logMessage) {
        JobStopMessage message = new JobStopMessage();

        message.jobId = new Double((double) logMessage.get("job_id")).intValue();
        message.endedAt = new Date(Long.valueOf((String) logMessage.get("ended_at")));
        message.jobStatus = (String) logMessage.get("job_status");

        return message;

    }
}
