import com.rabbitmq.client.Connection;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.Connections;
import net.jodah.lyra.config.Config;
import net.jodah.lyra.config.RecoveryPolicy;
import net.jodah.lyra.util.Duration;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.*;
import simpleci.dispatcher.listener.JobToBuildLifecycleListener;
import simpleci.dispatcher.job.JobsCreator;
import simpleci.dispatcher.listener.WorkerStateListener;
import simpleci.dispatcher.listener.centrifugo.CentrifugoApi;
import simpleci.dispatcher.listener.centrifugo.CentrifugoListener;
import simpleci.dispatcher.listener.db.BuildLifecycleListener;
import simpleci.dispatcher.listener.db.DbJobListener;
import simpleci.dispatcher.message.WorkerInfoRequestMessage;
import simpleci.dispatcher.queue.JobProducer;
import simpleci.dispatcher.queue.LogConsumer;
import simpleci.dispatcher.queue.ServiceMessageProducer;
import simpleci.dispatcher.repository.Repository;
import simpleci.dispatcher.repository.UpdaterRepository;
import simpleci.dispatcher.settings.SettingsManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, TimeoutException {
        DiContainer container = new DiContainer();

        initParameters(container);
        waitForRabbitmq(container.get("parameters", AppParameters.class));
        waitForDatabase(container.get("parameters", AppParameters.class));
        waitForRedis(container.get("parameters", AppParameters.class));
        initApp(container);

        container.get("worker.message_producer", ServiceMessageProducer.class).send(new WorkerInfoRequestMessage());

        container.get("log_consumer", LogConsumer.class).consume();
    }

    private static void initApp(DiContainer container) {
        try {
            container.add("connection", createConnection(
                    container.get("parameters", AppParameters.class)));

            container.add("data_source", createDataSource(
                    container.get("parameters", AppParameters.class)));

            container.add("repository", new Repository(
                    container.get("data_source", DataSource.class)));

            container.add("repository.updater", new UpdaterRepository(
                    container.get("data_source", DataSource.class)));

            container.add("settings_manager", new SettingsManager(
                    container.get("repository", Repository.class)));

            container.add("job.producer", new JobProducer(
                    container.get("connection", Connection.class)));

            container.add("worker.message_producer", new ServiceMessageProducer(
                    container.get("connection", Connection.class)));

            container.add("job.creator", new JobsCreator(
                    container.get("repository", Repository.class)));

            container.add("listener.job_to_build", new JobToBuildLifecycleListener(
                    container.get("repository", Repository.class),
                    container.get("settings_manager", SettingsManager.class),
                    container.get("job.creator", JobsCreator.class),
                    container.get("job.producer", JobProducer.class)));

            container.add("listener.build", new BuildLifecycleListener(
                    container.get("repository.updater", UpdaterRepository.class)));

            container.add("listener.db_job", new DbJobListener(
                    container.get("repository.updater", UpdaterRepository.class)));

            container.add("centrifugo_api", new CentrifugoApi());

            container.add("listener.centrifugo", new CentrifugoListener(
                    container.get("repository", Repository.class),
                    container.get("centrifugo_api", CentrifugoApi.class)));

            container.add("listener.workers", new WorkerStateListener(
                    container.get("settings_manager", SettingsManager.class)));

            container.add("event_dispatcher", new EventDispatcher(
                    container.get("listener.job_to_build", JobToBuildLifecycleListener.class),
                    container.get("listener.build", BuildLifecycleListener.class),
                    container.get("listener.db_job", DbJobListener.class),
                    container.get("listener.centrifugo", CentrifugoListener.class),
                    container.get("listener.workers", WorkerStateListener.class),
                    container.get("worker.message_producer", ServiceMessageProducer.class)));

            container.add("log_consumer", new LogConsumer(
                    container.get("connection", Connection.class),
                    container.get("event_dispatcher", EventDispatcher.class)));

        } catch (IOException | TimeoutException e) {
            fail("Error create services", e);
        }
    }

    private static void fail(String message, Exception e) {
        logger.error(message, e);
        System.exit(1);
    }

    private static void waitForRedis(AppParameters parameters) {
        logger.info("Waiting for redis");
        if (!Utils.waitForPort(parameters.redisHost, parameters.redisPort, 10, 1000)) {
            logger.error("Failed connect to redis server");
            System.exit(0);
        }
    }

    private static void waitForDatabase(AppParameters parameters) {
        logger.info("Waiting for database");
        if (!Utils.waitForPort(parameters.databaseHost, parameters.databasePort, 10, 1000)) {
            logger.error("Failed connect to database server");
            System.exit(0);
        }
    }

    private static void waitForRabbitmq(AppParameters parameters) {
        logger.info("Waiting for rabbitmq");
        if (!Utils.waitForPort(parameters.rabbitmqHost, parameters.rabbitmqPort, 10, 1000)) {
            logger.error("Failed connect to rabbitmq server");
            System.exit(0);
        }
    }

    private static void initParameters(DiContainer container) {
        container.add("parameters", AppParameters.fromEnv());
    }

    private static DataSource createDataSource(AppParameters parameters) {
        BasicDataSource bds = new BasicDataSource();

        bds.setDriverClassName("com.mysql.jdbc.Driver");
        bds.setUrl(String.format("jdbc:mysql://localhost:%d/%s", parameters.databasePort, parameters.databaseName));
        bds.setUsername(parameters.databaselUser);
        bds.setPassword(parameters.databasePassword);
        bds.setInitialSize(5);

        return bds;
    }

    private static Connection createConnection(AppParameters parameters) throws IOException, TimeoutException {
        Config config = new Config()
                .withRecoveryPolicy(new RecoveryPolicy()
                        .withBackoff(Duration.seconds(1), Duration.seconds(30))
                        .withMaxAttempts(20));
        ConnectionOptions options = new ConnectionOptions()
                .withHost(parameters.rabbitmqHost)
                .withPort(parameters.rabbitmqPort)
                .withUsername(parameters.rabbitmqUser)
                .withPassword(parameters.rabbitmqPassword);

        return Connections.create(options, config);
    }
}
