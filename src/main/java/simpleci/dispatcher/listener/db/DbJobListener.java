package simpleci.dispatcher.listener.db;

import simpleci.dispatcher.entity.JobStatus;
import simpleci.dispatcher.message.JobOutputMessage;
import simpleci.dispatcher.message.JobStartedMessage;
import simpleci.dispatcher.message.JobStoppedMessage;
import simpleci.dispatcher.repository.UpdaterRepository;

public class DbJobListener {
    private final UpdaterRepository repository;

    public DbJobListener(UpdaterRepository repository) {
        this.repository = repository;
    }

    public void jobStart(JobStartedMessage message)
    {
        repository.jobStarted(message.jobId, JobStatus.RUNNING, message.startedAt);
    }

    public void jobStop(JobStoppedMessage message) {
        repository.jobEnded(message.jobId, message.jobStatus, message.stoppedAt);
    }

    public void jobOutput(JobOutputMessage message) {
        repository.jobLog(message.jobId, message.output);
    }
}
