package simpleci.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.entity.Job;
import simpleci.dispatcher.listener.JobToBuildLifecycleListener;
import simpleci.dispatcher.listener.WorkerStateListener;
import simpleci.dispatcher.listener.centrifugo.CentrifugoListener;
import simpleci.dispatcher.listener.db.BuildLifecycleListener;
import simpleci.dispatcher.listener.db.DbJobListener;
import simpleci.dispatcher.message.*;
import simpleci.dispatcher.message.event.BuildStopEvent;
import simpleci.dispatcher.queue.ServiceMessageProducer;

import java.util.Date;
import java.util.List;

public class EventDispatcher
{
    private final static Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

    private final JobToBuildLifecycleListener jobToBuildLifecycleListener;
    private final BuildLifecycleListener buildLifecycleListener;
    private final DbJobListener dbJobListener;
    private final CentrifugoListener centrifugoListener;
    private final WorkerStateListener workerStateListener;
    private final ServiceMessageProducer serviceMessageProducer;

    public EventDispatcher(
            JobToBuildLifecycleListener jobToBuildLifecycleListener,
            BuildLifecycleListener buildLifecycleListener,
            DbJobListener dbJobListener,
            CentrifugoListener centrifugoListener,
            WorkerStateListener workerStateListener,
            ServiceMessageProducer serviceMessageProducer) {
        this.jobToBuildLifecycleListener = jobToBuildLifecycleListener;
        this.buildLifecycleListener = buildLifecycleListener;
        this.dbJobListener = dbJobListener;
        this.centrifugoListener = centrifugoListener;
        this.workerStateListener = workerStateListener;
        this.serviceMessageProducer = serviceMessageProducer;

        this.jobToBuildLifecycleListener.setEventDispatcher(this);
    }

    public void onJobStart(JobStartedMessage message) {
        logger.info(String.format("Job started, {job: %d, worker: %s, at: %s}",
                message.jobId, message.workerId, message.startedAt.toString()));
        workerStateListener.jobStart(message);
        dbJobListener.jobStart(message);
        centrifugoListener.jobStart(message);
    }

    public void onJobOutput(JobOutputMessage message) {
        dbJobListener.jobOutput(message);
        centrifugoListener.jobOutput(message);
    }

    public void onJobStop(JobStoppedMessage message) {
        logger.info(String.format("Job ended, {job: %d, status: %s, worker: %s, at: %s}",
                message.jobId, message.jobStatus, message.workerId, message.stoppedAt.toString()));
        workerStateListener.jobStop(message);
        dbJobListener.jobStop(message);
        centrifugoListener.jobStop(message);
        jobToBuildLifecycleListener.jobStop(message);
    }

    public void onBuildStart(BuildRequestMessage message) {
        logger.info(String.format("Build %d started at %s", message.buildId, new Date().toString()));
        jobToBuildLifecycleListener.buildStart(message);
        buildLifecycleListener.buildStart(message);
    }

    public void onBuildStop(BuildStopEvent event) {
        logger.info(String.format("Build %d stopped with status %s at %s", event.buildId, event.status, new Date().toString()));
        buildLifecycleListener.buildStop(event);
    }

    public void onWorkerStart(WorkerStartedMessage message) {
        logger.info(String.format("Started worker %s at %s", message.workerId, message.startedAt.toString()));
        workerStateListener.workerStart(message);
    }

    public void onWorkerStop(WorkerStoppedMessage message) {
        logger.info(String.format("Started worker %s at %s", message.workerId, message.stoppedAt.toString()));
        workerStateListener.workerStop(message);
    }

    public void afterJobsCreated(List<Job> jobsList) {
        workerStateListener.afterJobsCreated(jobsList);
    }

    public void onWorkerInfo(WorkerInfoResponseMessage message) {
        logger.info(String.format("Worker info: id %s, type %s, jobs %s", message.workerId, message.workerType, message.jobs));
        workerStateListener.workerInfo(message);
    }

    public void onJobStopRequest(JobStopRequestMessage message) {
        logger.info(String.format("Job %d will stopped", message.jobId));
        serviceMessageProducer.send(message);
    }
}
