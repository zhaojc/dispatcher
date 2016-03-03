package simpleci.dispatcher.message;


import java.util.Map;

public class JobOutputMessage {
    public int jobId;
    public String output;

    public static JobOutputMessage fromJson(Map logMessage) {
        JobOutputMessage output = new JobOutputMessage();
        output.jobId  = new Double((double) logMessage.get("job_id")).intValue();
        output.output = (String) logMessage.get("output");

        return output;
    }

}
