package simpleci.dispatcher.listener.db;


import simpleci.dispatcher.message.BuildRequestMessage;
import simpleci.dispatcher.message.event.BuildStopEvent;
import simpleci.dispatcher.repository.UpdaterRepository;

import java.util.Date;

public class BuildLifecycleListener {
    private final UpdaterRepository repository;

    public BuildLifecycleListener(UpdaterRepository repository) {
        this.repository = repository;
    }

    public void buildStart(BuildRequestMessage event)
    {
        Date startedAt = new Date();
        repository.buildStarted(event.buildId, startedAt);

    }

    public void buildStop(BuildStopEvent event) {
        Date stoppedAt = new Date();
        repository.buildStopped(event.buildId, event.status, stoppedAt);
    }
}
