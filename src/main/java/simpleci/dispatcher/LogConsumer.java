package simpleci.dispatcher;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.message.BuildMessage;
import simpleci.dispatcher.message.JobOutputMessage;
import simpleci.dispatcher.message.JobStartMessage;
import simpleci.dispatcher.message.JobStopMessage;

import java.io.IOException;
import java.util.Map;

public class LogConsumer {
    final static Logger logger = LoggerFactory.getLogger(LogConsumer.class);

    private static final String QUEUE_NAME = "log";
    private final EventDispatcher eventDispatcher;
    private final Connection connection;

    public LogConsumer(Connection connection, EventDispatcher eventDispatcher) throws IOException {
        this.connection = connection;
        this.eventDispatcher = eventDispatcher;
    }

    private Channel createChannel(Connection connection) throws IOException {
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(QUEUE_NAME, "direct", true);
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.queueBind(QUEUE_NAME, QUEUE_NAME, "");

        return channel;
    }

    public void consume() throws IOException {
        final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        final Channel channel = createChannel(connection);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                try {
                    String message = new String(body, "UTF-8");
                    Map logMessage = gson.fromJson(message, Map.class);
                    processMessage(logMessage);
                } catch (JsonSyntaxException e) {
                    logger.error("Error in json message", e);
                }
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }

    private void processMessage(Map logMessage) {
        if (!logMessage.containsKey("type")) {
            logger.error("Message must contains type field: " + logMessage.toString());
            return;
        }
        Object messageObj = convertMessage(logMessage);
        if (messageObj != null) {
            handleMessage(messageObj);
        }
    }

    private Object convertMessage(Map logMessage) {
        String messageType = (String) logMessage.get("type");
        switch (messageType) {
            case "build":
                return BuildMessage.fromJson(logMessage);
            case "job":
                return convertJobMessage(logMessage);
            default:
                logger.error("Unknown message type: " +  logMessage.toString());
                return null;
        }
    }

    private Object convertJobMessage(Map logMessage) {
        if (!logMessage.containsKey("action")) {
            logger.error("Log message must contains action field: " + logMessage);
            return null;
        }
        String logAction = (String) logMessage.get("action");
        switch (logAction) {
            case "start":
                return JobStartMessage.fromJson(logMessage);
            case "stop":
                return JobStopMessage.fromJson(logMessage);
            case "output":
                return JobOutputMessage.fromJson(logMessage);
            default:
                logger.error("Unknown log action: " + logMessage.toString());
                return null;
        }
    }

    private void handleMessage(Object messageObj) {
        if (messageObj instanceof BuildMessage) {
            eventDispatcher.onBuildStart((BuildMessage) messageObj);
        } else if (messageObj instanceof JobStartMessage) {
            eventDispatcher.onJobStart((JobStartMessage) messageObj);
        } else if (messageObj instanceof JobStopMessage) {
            eventDispatcher.onJobStop((JobStopMessage) messageObj);
        } else if (messageObj instanceof JobOutputMessage) {
            eventDispatcher.onJobOutput((JobOutputMessage) messageObj);
        }
    }

}
