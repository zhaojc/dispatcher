package simpleci.dispatcher.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.AppParameters;
import simpleci.dispatcher.entity.Job;
import simpleci.dispatcher.message.*;
import simpleci.dispatcher.settings.Settings;
import simpleci.dispatcher.settings.SettingsManager;

import java.util.*;

public class WorkerStateListener {
    private final static Logger logger = LoggerFactory.getLogger(WorkerStateListener.class);
    private final SettingsManager settingsManager;
    private final AppParameters parameters;

    public WorkerStateListener(AppParameters parameters, SettingsManager settingsManager) {
        this.parameters = parameters;
        this.settingsManager = settingsManager;
    }

    // worker id -> set of running jobs
    private Map<String, WorkerDescription> workers = new HashMap<>();

    public void workerStart(WorkerStartedMessage message) {
        if (workers.containsKey(message.workerId)) {
            logger.error(String.format("Worker %s already exists", message.workerId));
            return;
        }
        workers.put(message.workerId, new WorkerDescription(message.startedAt, message.workerType));
    }

    public void jobStart(JobStartedMessage message) {
        if (!workers.containsKey(message.workerId)) {
            logger.error(String.format("Worker %s does not exists", message.workerId));
            return;
        }
        workers.get(message.workerId).addJob(message.jobId);
    }

    public void jobStop(JobStoppedMessage message) {
        if (!workers.containsKey(message.workerId)) {
            logger.error(String.format("Worker %s does not exists", message.workerId));
            return;
        }
        if (!workers.get(message.workerId).hasJob(message.jobId)) {
            logger.error(String.format("Worker %s has not job %d", message.workerId, message.jobId));
            return;
        }

        workers.get(message.workerId).removeJob(message.jobId);
    }

    public void afterJobsCreated(List<Job> jobsList) {
        if (jobsList.isEmpty()) {
            return;
        }

        Settings settings = settingsManager.loadSettings();
        long buildId = jobsList.get(0).buildId;
        if(settings.useGce) {
            int instanceCount = jobsList.size() - freeWorkers();
            if (instanceCount > 0) {
                createInstances(buildId, instanceCount, settings);
            }
        }
    }

    private void createInstances(final long buildId, int instanceCount, Settings settings) {
        logger.info(String.format("Will create %d instances on gce", instanceCount));

        final GceApi gceApi = new GceApi(settings, parameters);
        List<Thread> createINstaceThreads = new ArrayList<>();
        for (int i = 0; i < instanceCount; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    gceApi.createInstance(String.format("worker-%d-%s", buildId, UUID.randomUUID()));
                }
            });
            createINstaceThreads.add(thread);
        }
        for (Thread thread : createINstaceThreads) {
            thread.start();
        }

    }

    private int freeWorkers() {
        int freeWorkers = 0;
        for (Map.Entry<String, WorkerDescription> entry : workers.entrySet()) {
            if (!entry.getValue().hasJobs()) {
                freeWorkers++;
            }
        }
        return freeWorkers;
    }

    public void workerStop(final WorkerStoppedMessage message) {
        workers.remove(message.workerId);
        Settings settings = settingsManager.loadSettings();

        if (message.workerType.equals("gce")) {
            final GceApi gce = new GceApi(settings, parameters);
            Thread stopThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    gce.stopAndRemoveInstance(message.workerHostName);
                }
            });
            stopThread.start();
        }
    }

    public void workerInfo(WorkerInfoResponseMessage message) {
        if(workers.containsKey(message.workerId)) {
            workers.remove(message.workerId);
        }
        WorkerDescription workerDescription = new WorkerDescription(message.startedAt, message.workerType);
        for(Integer job : message.jobs) {
            workerDescription.addJob(job);
        }
        workers.put(message.workerId, workerDescription);

    }
}
