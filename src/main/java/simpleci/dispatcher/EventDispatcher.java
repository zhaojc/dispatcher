package simpleci.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.listener.JobToBuildLifecycleListener;
import simpleci.dispatcher.listener.centrifugo.CentrifugoListener;
import simpleci.dispatcher.listener.db.BuildLifecycleListener;
import simpleci.dispatcher.listener.db.DbJobListener;
import simpleci.dispatcher.message.*;
import simpleci.dispatcher.message.event.BuildStopEvent;

import java.util.Date;

public class EventDispatcher
{
    private final static Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

    private final JobToBuildLifecycleListener jobToBuildLifecycleListener;
    private final BuildLifecycleListener buildLifecycleListener;
    private final DbJobListener dbJobListener;
    private final CentrifugoListener centrifugoListener;

    public EventDispatcher(
            JobToBuildLifecycleListener jobToBuildLifecycleListener,
            BuildLifecycleListener buildLifecycleListener,
            DbJobListener dbJobListener,
            CentrifugoListener centrifugoListener) {
        this.jobToBuildLifecycleListener = jobToBuildLifecycleListener;
        this.buildLifecycleListener = buildLifecycleListener;
        this.dbJobListener = dbJobListener;
        this.centrifugoListener = centrifugoListener;

        this.jobToBuildLifecycleListener.setEventDispatcher(this);
    }

    public void onJobStart(JobStartMessage message) {
        logger.info(String.format("Job %d started at %s", message.jobId, message.startedAt.toString()));
        dbJobListener.jobStart(message);
        centrifugoListener.jobStart(message);
    }

    public void onJobOutput(JobOutputMessage message) {
        dbJobListener.jobOutput(message);
        centrifugoListener.jobOutput(message);
    }

    public void onJobStop(JobStopMessage message) {
        logger.info(String.format("Job %d ended at %s", message.jobId, message.endedAt.toString()));
        dbJobListener.jobStop(message);
        centrifugoListener.jobStop(message);
        jobToBuildLifecycleListener.jobStop(message);
    }

    public void onBuildStart(BuildMessage message) {
        logger.info(String.format("Build %d started at %s", message.buildId, new Date().toString()));
        jobToBuildLifecycleListener.buildStart(message);
        buildLifecycleListener.buildStart(message);
    }

    public void onBuildStop(BuildStopEvent event) {
        logger.info(String.format("Build %d stopped at %s", event.buildId, new Date().toString()));
        buildLifecycleListener.buildStop(event);
    }
}
