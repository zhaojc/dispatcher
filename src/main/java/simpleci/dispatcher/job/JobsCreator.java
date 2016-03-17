package simpleci.dispatcher.job;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import simpleci.dispatcher.Tuple;
import simpleci.dispatcher.entity.Build;
import simpleci.dispatcher.entity.Job;
import simpleci.dispatcher.entity.JobStatus;
import simpleci.dispatcher.repository.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobsCreator {
    private final Repository repository;

    public JobsCreator(Repository repository) {
        this.repository = repository;
    }

    public List<Tuple<Job, Map>> createJobs(Build build, String stage) {
        final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        List<Map> jobsConfig = new JobsConfigGenerator().generateJobsConfig(build, stage);

        List<Tuple<Job, Map>> jobs = new ArrayList<>();
        for (Map jobConfig : jobsConfig) {
            Job job = new Job();
            job.number = repository.getJobMaxNumber(build.id) + 1;
            job.buildId = build.id;
            job.status = JobStatus.PENDING;
            job.stage = stage;
            job.parameters = gson.toJson(jobConfig.get("cell"));

            jobs.add(new Tuple<>(job, jobConfig));
        }
        return jobs;
    }


}
