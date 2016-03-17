package simpleci.dispatcher.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.entity.Build;
import simpleci.dispatcher.entity.Job;
import simpleci.dispatcher.entity.Project;
import simpleci.dispatcher.entity.SettingsParameter;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Repository {
    final static Logger logger = LoggerFactory.getLogger(Repository.class);

    private final DataSource dataSource;

    public Repository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Build findBuild(long id) {
        String query = "SELECT id, project_id, number, branch, commit, commit_range, config from build where id = ?";
        try {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setLong(1, id);
                    try (ResultSet resultSet = statement.executeQuery()) {

                        while (resultSet.next()) {
                            Build build = new Build();
                            build.id = resultSet.getLong("id");
                            build.projectId = resultSet.getLong("project_id");
                            build.commit = resultSet.getString("commit");
                            build.commitRange = resultSet.getString("commit_range");
                            build.branch = resultSet.getString("branch");
                            build.number = resultSet.getInt("number");
                            build.config = resultSet.getString("config");
                            return build;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("", e);
        }
        return null;
    }

    public int getJobMaxNumber(long buildId) {
        String query = "SELECT MAX(number) as maxNumber from job where job.build_id = ?";
        try {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setLong(1, buildId);
                    try (ResultSet resultSet = statement.executeQuery()) {

                        while (resultSet.next()) {
                            return resultSet.getInt("maxNumber");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("", e);
        }
        return 0;

    }

    public void insertJob(Job job) {
        try {
            String query = "INSERT INTO job(number, build_id, status, stage, parameters, log) values(?, ?, ?, ?, ?, '')";
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, job.number);
                    statement.setLong(2, job.buildId);
                    statement.setString(3, job.status);
                    statement.setString(4, job.stage);
                    statement.setString(5, job.parameters);
                    statement.executeUpdate();

                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        keys.next();
                        job.id = keys.getInt(1);
                    }

                }
            }
        } catch (SQLException e) {
            logger.error("", e);
        }
    }

    public Project findProject(long projectId) {
        String query = "SELECT id, public_key, private_key, repository_url from project where id = ?";
        try {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setLong(1, projectId);
                    try (ResultSet resultSet = statement.executeQuery()) {

                        while (resultSet.next()) {
                            Project project = new Project();
                            project.id = resultSet.getLong("id");
                            project.publicKey = resultSet.getString("public_key");
                            project.privateKey = resultSet.getString("private_key");
                            project.repositoryUrl = resultSet.getString("repository_url");
                            return project;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("", e);
        }
        return null;
    }

    public Job findJob(int jobId) {
        String query = "SELECT id, build_id, status, stage from job where id = ?";
        try {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setLong(1, jobId);
                    try (ResultSet resultSet = statement.executeQuery()) {

                        while (resultSet.next()) {
                            Job job = new Job();
                            job.id = resultSet.getLong("id");
                            job.buildId = resultSet.getLong("build_id");
                            job.status = resultSet.getString("status");
                            job.stage = resultSet.getString("stage");
                            return job;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("", e);
        }
        return null;
    }

    public List<Job> buildJobs(long buildId) {
        String query = "SELECT id, build_id, status, stage from job where job.build_id = ?";
        try {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setLong(1, buildId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        List<Job> jobs = new ArrayList<>();

                        while (resultSet.next()) {
                            Job job = new Job();
                            job.id = resultSet.getLong("id");
                            job.buildId = resultSet.getLong("build_id");
                            job.status = resultSet.getString("status");
                            job.stage = resultSet.getString("stage");
                            jobs.add(job);
                        }
                        return jobs;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("", e);
        }
        return new ArrayList<>();
    }

    public List<SettingsParameter> getSettings() {
        String query = "SELECT id, name, value from settings_parameter";
        try {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        List<SettingsParameter> parameters = new ArrayList<>();

                        while (resultSet.next()) {
                            SettingsParameter parameter = new SettingsParameter();
                            parameter.id = resultSet.getLong("id");
                            parameter.name = resultSet.getString("name");
                            parameter.value = resultSet.getString("value");
                            parameters.add(parameter);
                        }
                        return parameters;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("", e);
        }
        return new ArrayList<>();
    }


}
