package simpleci.dispatcher.message;

import com.google.common.base.MoreObjects;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static simpleci.dispatcher.JsonUtils.jsonArrayToIntList;
import static simpleci.dispatcher.JsonUtils.tsToDate;

public class WorkerInfoResponseMessage {
    public final String workerId;
    public final String workerType;
    public final Date startedAt;
    public final List<Integer> jobs;

    public WorkerInfoResponseMessage(JsonObject message) {
        workerId = message.get("worker_id").getAsString();
        workerType = message.get("worker_type").getAsString();
        startedAt = tsToDate(message.get("started_at").getAsString());
        jobs = jsonArrayToIntList(message.get("jobs").getAsJsonArray());
    }

}
