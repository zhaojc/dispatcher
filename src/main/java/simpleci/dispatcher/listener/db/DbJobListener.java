package simpleci.dispatcher.listener.db;

import simpleci.dispatcher.entity.JobStatus;
import simpleci.dispatcher.message.JobOutputMessage;
import simpleci.dispatcher.message.JobStartMessage;
import simpleci.dispatcher.message.JobStopMessage;
import simpleci.dispatcher.repository.UpdaterRepository;

public class DbJobListener {
    private final UpdaterRepository repository;

    public DbJobListener(UpdaterRepository repository) {
        this.repository = repository;
    }

    public void jobStart(JobStartMessage message)
    {
        repository.jobStarted(message.jobId, JobStatus.RUNNING, message.startedAt);
    }

    public void jobStop(JobStopMessage message) {
        repository.jobEnded(message.jobId, message.jobStatus, message.endedAt);
    }

    public void jobOutput(JobOutputMessage message) {
        repository.jobLog(message.jobId, message.output);
    }
}
