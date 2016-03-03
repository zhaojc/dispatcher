package simpleci.dispatcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class AppParameters
{
    public String rabbitmqHost;
    public int rabbitmqPort;
    public String rabbitmqUser;
    public String rabbitmqPassword;

    public String redisHost;
    public int redisPort;

    public String databaseHost;
    public int databasePort;
    public String databaselUser;
    public String databasePassword;
    public String databaseName;

    public static AppParameters fromEnv() {
        AppParameters parameters = new AppParameters();

        parameters.rabbitmqHost = getEnv("RABBITMQ_HOST", "localhost");
        parameters.rabbitmqPort = getEnv("RABBITMQ_PORT", 5672);
        parameters.rabbitmqUser = getEnv("RABBITMQ_USER", "guest");
        parameters.rabbitmqPassword = getEnv("RABBITMQ_PASSWORD", "guest");

        parameters.redisHost = getEnv("REDIS_HOST", "localhost");
        parameters.redisPort = getEnv("REDIS_PORT", 6379);

        parameters.databaseHost = getEnv("DATABASE_HOST", "localhost");
        parameters.databasePort = getEnv("DATABASE_PORT", 3306);
        parameters.databaselUser = getEnv("DATABASE_USER", "root");
        parameters.databasePassword = getEnv("DATABASE_PASSWORD", "");
        parameters.databaseName = getEnv("DATABASE_NAME", "simpleci");

        return parameters;
    }

    private static int getEnv(String name, int defaultValue) {
        String envValue = System.getenv(name);
        if(envValue != null) {
            return Integer.parseInt(envValue);
        }
        return defaultValue;
    }

    private static String getEnv(String name, String defaultValue) {
        String envValue = System.getenv(name);
        if(envValue != null) {
            return envValue;
        }
        return defaultValue;
    }

 }
