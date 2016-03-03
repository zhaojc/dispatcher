package simpleci.dispatcher;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.entity.Build;
import simpleci.dispatcher.entity.Job;
import simpleci.dispatcher.entity.Project;

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

    public void newJob(Project project, Build build, Job job, Map jobConfig) {
        ImmutableMap<String, Object> jobMessage = new ImmutableMap.Builder<String, Object>()
                .put("project_settings", makeProjectSettings(project))
                .put("job_config", jobConfig)
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
                .put("repository_url", project.repositoryUrl)
                .build();
        send(jobMessage);

    }

    private Object makeProjectSettings(Project project) {
        return new ImmutableMap.Builder<String, Object>()
                .put("cache_type", project.settingsCacheType)
                .put("ssh_host", project.settingsCacheSshHost)
                .put("ssh_user", project.settingsCacheSshUser)
                .put("ssh_dir", project.settingsCacheSshDir)
                .build();
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
