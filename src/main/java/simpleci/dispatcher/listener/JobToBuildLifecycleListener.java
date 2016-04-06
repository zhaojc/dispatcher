package simpleci.dispatcher.listener;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

import java.util.*;

public class JobToBuildLifecycleListener {
    private final static Logger logger = LoggerFactory.getLogger(JobToBuildLifecycleListener.class);
    private final SettingsManager settingsManager;

    private final Repository repository;
    private final JobsCreator jobsCreator;
    private final JobProducer jobProducer;
    private final Gson gson;
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
        this.gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    public void setEventDispatcher(EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    public void buildStart(BuildRequestMessage message) {
        final long buildId = message.buildId;

        Build build = repository.findBuild(buildId);
        Project project = repository.findProject(build.projectId);

        Map config = gson.fromJson(build.config, Map.class);
        if(!config.containsKey("stages")) {
            logger.error("Build config does not contains stages section");
            return;
        }
        List<String> stages = (List<String>) config.get("stages");
        if(stages.size() == 0) {
            logger.error("It must be at least one stage");
            return;
        }

        createBuildJobs(build, project, config, stages.get(0));
    }

    private Map<String, String> createStageTransitions(List<String> stages) {
        Map<String, String> transitions = new HashMap<>();
        if(stages.size() <= 1) {
            return transitions;
        }

        for(int i = 0; i < stages.size() - 1; i++) {
            transitions.put(stages.get(i), stages.get(i + 1));
        }
        return transitions;
    }

    private List<Job> createBuildJobs(Build build, Project project, Map config, String stage) {
        List<Tuple<Job, Map>> jobs = jobsCreator.createJobs(build, config, stage);
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
        Project project = repository.findProject(build.projectId);

        List<Job> jobs = repository.buildJobs(build.id);

        if (allJobsAreFinished(jobs)) {
            String buildStatus = buildStatus(jobs);
            if (buildStatus.equals(JobStatus.FINISHED_SUCCESS)) {
                Map config = gson.fromJson(build.config, Map.class);
                List<String> stages = (List<String>) config.get("stages");
                Map<String, String> stageTransitions = createStageTransitions(stages);

                if (stageTransitions.containsKey(job.stage)) {
                    String nextStage = stageTransitions.get(job.stage);
                    List<Job> createdJobs = createBuildJobs(build, project, config, nextStage);

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
