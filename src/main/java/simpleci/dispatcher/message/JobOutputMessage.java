package simpleci.dispatcher.message;


import com.google.gson.JsonObject;

public class JobOutputMessage {
    public final int jobId;
    public final String output;

    public JobOutputMessage(JsonObject message) {
        jobId  = message.get("job_id").getAsInt();
        output = message.get("output").getAsString();
    }

}
