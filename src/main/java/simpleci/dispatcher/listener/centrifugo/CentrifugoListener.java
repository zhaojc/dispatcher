package simpleci.dispatcher.listener.centrifugo;

import com.google.common.collect.ImmutableMap;
import simpleci.dispatcher.repository.Repository;
import simpleci.dispatcher.entity.Build;
import simpleci.dispatcher.entity.Job;
import simpleci.dispatcher.entity.Project;
import simpleci.dispatcher.message.JobOutputMessage;
import simpleci.dispatcher.message.JobStartedMessage;
import simpleci.dispatcher.message.JobStoppedMessage;

import java.util.Map;

public class CentrifugoListener {
    private final Repository repository;
    private final CentrifugoApi api;

    public CentrifugoListener(Repository repository, CentrifugoApi api) {
        this.repository = repository;
        this.api = api;
    }

    public void jobStart(JobStartedMessage message) {
        Job job = repository.findJob(message.jobId);
        Build build = repository.findBuild(job.buildId);
        Project project = repository.findProject(build.projectId);

        Map<String, Object> sendMessage = new ImmutableMap.Builder<String, Object>()
                .put("project_id", project.id)
                .put("build_id", build.id)
                .put("job_id", job.id)
                .put("action", "job_start")
                .put("started_at", message.startedAt.getTime())
                .build();
        api.send(sendMessage, projectChannelName(project.id));
    }

    public void jobOutput(JobOutputMessage message) {
        api.send(message.output, jobChannelName(message.jobId));
    }

    public void jobStop(JobStoppedMessage message) {
        Job job = repository.findJob(message.jobId);
        Build build = repository.findBuild(job.buildId);
        Project project = repository.findProject(build.projectId);

        Map<String, Object> sendMessage = new ImmutableMap.Builder<String, Object>()
                .put("project_id", project.id)
                .put("build_id", build.id)
                .put("job_id", job.id)
                .put("action", "job_stop")
                .put("ended_at", message.stoppedAt.getTime())
                .put("job_status", message.jobStatus)
                .build();
        api.send(sendMessage, projectChannelName(project.id));
    }

    private String projectChannelName(long projectId) {
        return "$project." + projectId;
    }

    private String jobChannelName(long jobId) {
        return "$job." + jobId;
    }
}
