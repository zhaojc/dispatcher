package simpleci.dispatcher.listener.db;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.repository.Repository;
import simpleci.dispatcher.entity.JobStatus;
import simpleci.dispatcher.message.BuildMessage;
import simpleci.dispatcher.message.event.BuildStopEvent;
import simpleci.dispatcher.repository.UpdaterRepository;

import java.util.Date;

public class BuildLifecycleListener {
    private final UpdaterRepository repository;

    public BuildLifecycleListener(UpdaterRepository repository) {
        this.repository = repository;
    }

    public void buildStart(BuildMessage event)
    {
        Date startedAt = new Date();
        repository.buildStarted(event.buildId, JobStatus.RUNNING, startedAt);

    }

    public void buildStop(BuildStopEvent event) {
        Date stoppedAt = new Date();
        repository.buildStopped(event.buildId, JobStatus.RUNNING, stoppedAt);
    }
}
