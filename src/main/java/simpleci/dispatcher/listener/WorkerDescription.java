package simpleci.dispatcher.listener;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class WorkerDescription {
    public final Date startedAt;
    public final String type;
    private final Set<Integer> jobs = new HashSet<>();

    public WorkerDescription(Date startedAt, String type) {
        this.startedAt = startedAt;
        this.type = type;
    }

    public void addJob(int jobId) {
        jobs.add(jobId);
    }

    public boolean hasJob(int jobId) {
        return jobs.contains(jobId);
    }

    public boolean hasJobs() {
        return !jobs.isEmpty();
    }

    public void removeJob(int jobId) {
        jobs.remove(jobId);
    }
}
