import com.rabbitmq.client.Connection;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.Connections;
import net.jodah.lyra.config.Config;
import net.jodah.lyra.config.RecoveryPolicy;
import net.jodah.lyra.util.Duration;
import org.apache.commons.dbcp2.BasicDataSource;
import simpleci.dispatcher.*;
import simpleci.dispatcher.listener.JobToBuildLifecycleListener;
import simpleci.dispatcher.JobsCreator;
import simpleci.dispatcher.listener.centrifugo.CentrifugoApi;
import simpleci.dispatcher.listener.centrifugo.CentrifugoListener;
import simpleci.dispatcher.listener.db.BuildLifecycleListener;
import simpleci.dispatcher.listener.db.DbJobListener;
import simpleci.dispatcher.repository.Repository;
import simpleci.dispatcher.repository.UpdaterRepository;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) throws IOException, TimeoutException {
        AppParameters parameters = AppParameters.fromEnv();
        Connection connection = createConnection(parameters);
        DataSource dataSource = createDataSource(parameters);
        Repository repository = new Repository(dataSource);
        UpdaterRepository updaterRepository = new UpdaterRepository(dataSource);
        JobProducer jobProducer = new JobProducer(connection);

        JobsCreator jobsCreator = new JobsCreator(repository, jobProducer);
        JobToBuildLifecycleListener jobToBuildListener = new JobToBuildLifecycleListener(repository, jobsCreator);
        BuildLifecycleListener buildLifecycleListener = new BuildLifecycleListener(updaterRepository);
        DbJobListener dbJobListener = new DbJobListener(updaterRepository);
        CentrifugoApi centrifugoApi = new CentrifugoApi();
        CentrifugoListener centrifugoJobLifecycleListener = new CentrifugoListener(repository, centrifugoApi);
        EventDispatcher eventDispatcher = new EventDispatcher(
                jobToBuildListener,
                buildLifecycleListener,
                dbJobListener,
                centrifugoJobLifecycleListener);

        LogConsumer logConsumer = new LogConsumer(connection, eventDispatcher);
        logConsumer.consume();

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
