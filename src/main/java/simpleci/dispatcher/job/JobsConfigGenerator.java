package simpleci.dispatcher.job;

import com.google.common.collect.ImmutableSet;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.entity.Build;

import java.util.*;

public class JobsConfigGenerator
{
    final static Logger logger = LoggerFactory.getLogger(JobsConfigGenerator.class);

    private static final String DEFAULT_SECTION = "build";

    public List<Map> generateJobsConfig(Build build, String section)
    {
        final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        Map config = gson.fromJson(build.config, Map.class);
        if (!section.equals(DEFAULT_SECTION)) {
            if(!config.containsKey(section)) {
                logger.info(String.format("Config for build %d does not contains section %s, skipping", build.id, section));
                return new ArrayList<>();
            }

            config = (Map) config.get(section);
        }

        if(!config.containsKey("matrix")) {
            logger.error("Job section must contain a matrix");
            return new ArrayList<>();
        }

        List matrix = (List) config.get("matrix");
        config.remove("matrix");

        List<Map> validMatrixCells = filterValidCells(matrix, build);
        List<Map> jobsConfig = new ArrayList<>();
        for(Map matrixCell : validMatrixCells) {
            Map jobConfig = new HashMap(config);
            jobConfig.put("cell", matrixCell);
            jobsConfig.add(jobConfig);
        }
       return  jobsConfig;
    }

    private List<Map> filterValidCells(List<Map> matrix, Build build) {
        List<Map> validCells = new ArrayList<Map>();
        for(Map matrixElem : matrix) {
            if(isValidCell(matrixElem, build)) {
                validCells.add(matrixElem);
            }
        }
        return validCells;
    }

    private boolean isValidCell(Map matrixCell, Build build)
    {
        if (!matrixCell.containsKey("on")) {
            return true;
        }

        Map onCond = (Map) matrixCell.get("on");
        if (onCond.containsKey("branch")) {
            Object branchCond = onCond.get("branch");
            Set<String> allowedBranches;
            if(branchCond instanceof String) {
                allowedBranches = ImmutableSet.of((String) branchCond);
            } else {
                allowedBranches = ImmutableSet.copyOf((List<String>)branchCond);
            }
            if(!allowedBranches.contains(build.branch)) {
                return false;
            }

        }
        return true;
    }
}
