package simpleci.dispatcher.listener;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.EventDispatcher;
import simpleci.dispatcher.queue.JobProducer;
import simpleci.dispatcher.job.JobsCreator;
import simpleci.dispatcher.Tuple;
import simpleci.dispatcher.entity.Project;
import simpleci.dispatcher.repository.Repository;
import simpleci.dispatcher.entity.Build;
import simpleci.dispatcher.entity.Job;
import simpleci.dispatcher.entity.JobStatus;
import simpleci.dispatcher.message.BuildRequestMessage;
import simpleci.dispatcher.message.JobStoppedMessage;
import simpleci.dispatcher.message.event.BuildStopEvent;
import simpleci.dispatcher.settings.SettingsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JobToBuildLifecycleListener {
    private final static Logger logger = LoggerFactory.getLogger(JobToBuildLifecycleListener.class);
    private final SettingsManager settingsManager;

    private Map<String, String> JOB_TRANSITIONS = new ImmutableMap.Builder<String, String>()
            .put("build", "deploy")
            .build();

    private final Repository repository;
    private final JobsCreator jobsCreator;
    private final JobProducer jobProducer;
    private EventDispatcher eventDispatcher;

    public JobToBuildLifecycleListener(
            Repository repository,
            SettingsManager settingsManager,
            JobsCreator jobsCreator,
            JobProducer jobProducer) {
        this.repository = repository;
        this.settingsManager = settingsManager;
        this.jobsCreator = jobsCreator;
        this.jobProducer = jobProducer;
    }

    public void setEventDispatcher(EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    public void buildStart(BuildRequestMessage message) {
        createBuildJobs(message.buildId, "build");
    }

    private List<Job> createBuildJobs(long buildId, String stage) {
        Build build = repository.findBuild(buildId);
        Project project = repository.findProject(build.projectId);

        List<Tuple<Job, Map>> jobs = jobsCreator.createJobs(build, stage);
        for (Tuple<Job, Map> job : jobs) {
            repository.insertJob(job.x);
            jobProducer.newJob(project, build, job.x, job.y, settingsManager.loadSettings());

            logger.info(String.format("Created job %d for build %d", job.x.id, build.id));
        }

        final List<Job> jobsList = new ArrayList<>();
        for (Tuple<Job, Map> job : jobs) {
            jobsList.add(job.x);
        }
        eventDispatcher.afterJobsCreated(jobsList);

        return jobsList;
    }

    public void jobStop(JobStoppedMessage message) {
        Job job = repository.findJob(message.jobId);
        Build build = repository.findBuild(job.buildId);
        List<Job> jobs = repository.buildJobs(build.id);

        if (allJobsAreFinished(jobs)) {
            String buildStatus = buildStatus(jobs);
            if (buildStatus.equals(JobStatus.FINISHED_SUCCESS)) {
                if (JOB_TRANSITIONS.containsKey(job.stage)) {
                    String nextStage = JOB_TRANSITIONS.get(job.stage);
                    List<Job> createdJobs = createBuildJobs(build.id, nextStage);

                    if (!createdJobs.isEmpty()) {
                        // Do nothing - new jobs created
                        return;
                    }
                }
            }

            BuildStopEvent event = new BuildStopEvent(build.id, message.stoppedAt, buildStatus);
            eventDispatcher.onBuildStop(event);
        }
    }

    private boolean allJobsAreFinished(List<Job> jobs) {
        Set<String> finishedStates = new ImmutableSet.Builder<String>()
                .add(JobStatus.FINISHED_SUCCESS).add(JobStatus.FAILED).add(JobStatus.STOPPED)
                .build();

        for (Job buildJob : jobs) {
            if (!finishedStates.contains(buildJob.status)) {
                return false;
            }
        }
        return true;
    }

    private String buildStatus(List<Job> buildJobs) {
        for (Job job : buildJobs) {
            if (!job.status.equals(JobStatus.FINISHED_SUCCESS)) {
                return job.status;
            }
        }
        return JobStatus.FINISHED_SUCCESS;
    }
}
