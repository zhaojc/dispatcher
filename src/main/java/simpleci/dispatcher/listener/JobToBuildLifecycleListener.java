package simpleci.dispatcher.listener;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.EventDispatcher;
import simpleci.dispatcher.JobsCreator;
import simpleci.dispatcher.repository.Repository;
import simpleci.dispatcher.entity.Build;
import simpleci.dispatcher.entity.Job;
import simpleci.dispatcher.entity.JobStatus;
import simpleci.dispatcher.message.BuildMessage;
import simpleci.dispatcher.message.JobStopMessage;
import simpleci.dispatcher.message.event.BuildStopEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class JobToBuildLifecycleListener {
    private Map<String, String> JOB_TRANSITIONS = new ImmutableMap.Builder<String, String>()
            .put("build", "deploy")
            .build();

    private final Repository repository;
    private final JobsCreator jobsCreator;
    private EventDispatcher eventDispatcher;

    public JobToBuildLifecycleListener(Repository repository, JobsCreator jobsCreator) {
        this.repository = repository;
        this.jobsCreator = jobsCreator;
    }

    public void setEventDispatcher(EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    public void buildStart(BuildMessage message) {
        jobsCreator.createJobs(message.buildId, "build");
    }

    public void jobStop(JobStopMessage message) {
        Job job = repository.findJob(message.jobId);
        Build build = repository.findBuild(job.buildId);
        List<Job> jobs = repository.buildJobs(build.id);

        if (allJobsAreFinished(jobs)) {
            String buildStatus = buildStatus(jobs);
            if (buildStatus.equals(JobStatus.FINISHED_SUCCESS)) {
                if (JOB_TRANSITIONS.containsKey(job.stage)) {
                    String nextStage = JOB_TRANSITIONS.get(job.stage);
                    List<Job> createdJobs = jobsCreator.createJobs(build.id, nextStage);

                    if (!createdJobs.isEmpty()) {
                        // Do nothing - new jobs created
                        return;
                    }
                }
            }

            BuildStopEvent event = new BuildStopEvent(build.id, message.endedAt, buildStatus);
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
