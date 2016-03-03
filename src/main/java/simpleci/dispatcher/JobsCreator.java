package simpleci.dispatcher;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.entity.Build;
import simpleci.dispatcher.entity.Job;
import simpleci.dispatcher.entity.JobStatus;
import simpleci.dispatcher.entity.Project;
import simpleci.dispatcher.repository.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobsCreator {
    final static Logger logger = LoggerFactory.getLogger(JobsCreator.class);

    private final Repository repository;
    private final JobProducer jobProducer;

    public JobsCreator(Repository repository, JobProducer jobProducer) {
        this.repository = repository;
        this.jobProducer = jobProducer;
    }

    public List<Job> createJobs(long buildId, String stage) {
        Build build = repository.findBuild(buildId);
        Project project = repository.findProject(build.projectId);

        return createJobs(project, build, stage);
    }

    private List<Job> createJobs(Project project, Build build, String stage) {
        final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        List<Map> jobsConfig = new JobsConfigGenerator().generateJobsConfig(build);

        List<Job> jobs = new ArrayList<>();
        for (Map jobConfig : jobsConfig) {
            Job job = new Job();
            job.number = repository.getJobMaxNumber(build.id) + 1;
            job.buildId = build.id;
            job.status = JobStatus.PENDING;
            job.stage = stage;
            job.parameters = gson.toJson(jobConfig.get("cell"));

            repository.insertJob(job);
            jobProducer.newJob(project, build, job, jobConfig);

            logger.info(String.format("Created job %d for build %d", job.id, build.id));
            jobs.add(job);
        }
        return jobs;
    }


}
