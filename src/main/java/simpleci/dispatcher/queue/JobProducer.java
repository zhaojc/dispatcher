package simpleci.dispatcher.queue;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.entity.Build;
import simpleci.dispatcher.entity.Job;
import simpleci.dispatcher.entity.Project;
import simpleci.dispatcher.settings.Settings;

import java.io.IOException;
import java.util.Map;

public class JobProducer {

    final Logger logger = LoggerFactory.getLogger(JobProducer.class);
    private static final String QUEUE_NAME = "job";

    private Channel channel;

    public JobProducer(Connection connection) throws IOException {
        this.channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
    }

    public void newJob(Project project, Build build, Job job, Map jobConfig, Settings settings) {
        ImmutableMap.Builder<String, Object> jobMessage = new ImmutableMap.Builder<String, Object>();
        jobMessage.put("job_config", jobConfig);
        jobMessage.put("job_settings", makeJobSettings(project, build, job, settings));

        send(jobMessage.build());

    }

    private Object makeJobSettings(Project project, Build build, Job job, Settings settings) {
        ImmutableMap.Builder<String, Object> jobSettings = new ImmutableMap.Builder<String, Object>();
        jobSettings
                .put("stage", job.stage)
                .put("job_id", job.id)
                .put("job_number", job.number)
                .put("build_id", job.buildId)
                .put("build_number", build.number)
                .put("project_id", project.id)
                .put("commit", build.commit)
                .put("commit_range", build.commitRange)
                .put("branch", build.branch)
                .put("public_key", project.publicKey)
                .put("private_key", project.privateKey)
                .put("repository_url", project.repositoryUrl);

        jobSettings
                .put("cache_type", settings.cacheType)
                .put("cache_options", makeCacheOptions(settings));

        return jobSettings.build();
    }

    private Object makeCacheOptions(Settings settings) {
        switch (settings.cacheType) {
            case "ssh":
                return new ImmutableMap.Builder<String, Object>()
                        .put("host", settings.cacheSshHost)
                        .put("user", settings.cacheSshUser)
                        .put("port", settings.cacheSshPort)
                        .put("dir", settings.cacheSshDir)
                        .put("public_key", settings.cacheSshPublicKey)
                        .put("private_key", settings.cacheSshPrivateKey)
                        .build();
            default:
                return emptyMap();
        }
    }

    private Object emptyMap() {
        return new ImmutableMap.Builder<String, Object>().build();
    }

    private Object emptyStringIfNull(Object object) {
        return object != null ? object : "";
    }

    private void send(Map<String, Object> message) {
        Gson gson = new Gson();
        String sendMessage = gson.toJson(message);
        try {
            channel.basicPublish("", QUEUE_NAME, null, sendMessage.getBytes());
        } catch (IOException e) {
            logger.error("Error publish log message", e);
        }
    }


}
