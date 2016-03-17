package simpleci.dispatcher.queue;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.EventDispatcher;
import simpleci.dispatcher.message.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class LogConsumer {
    private static final String QUEUE_NAME = "log";

    private final static Logger logger = LoggerFactory.getLogger(LogConsumer.class);
    private static final Map<String, Class> MESSAGE_TYPES = new ImmutableMap.Builder<String, Class>()
            .put("worker_started", WorkerStartedMessage.class)
            .put("worker_stopped", WorkerStoppedMessage.class)
            .put("worker_info_response", WorkerInfoResponseMessage.class)
            .put("job_started", JobStartedMessage.class)
            .put("job_stopped", JobStoppedMessage.class)
            .put("job_output", JobOutputMessage.class)
            .put("job_stop_request", JobStopRequestMessage.class)
            .put("build_request", BuildRequestMessage.class)
            .build();

    private final EventDispatcher eventDispatcher;
    private final Connection connection;

    public LogConsumer(Connection connection, EventDispatcher eventDispatcher) throws IOException {
        this.connection = connection;
        this.eventDispatcher = eventDispatcher;
    }

    private Channel createChannel(Connection connection) throws IOException {
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        return channel;
    }

    public void consume() throws IOException {
        final Channel channel = createChannel(connection);
        final JsonParser parser = new JsonParser();
        final Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                try {
                    String messageStr = new String(body, "UTF-8");
                    JsonObject message = parser.parse(messageStr).getAsJsonObject();
                    processMessage(message);
                } catch (JsonSyntaxException e) {
                    logger.error("Error in json message", e);
                }
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }

    private void processMessage(JsonObject logMessage) {
        if (!logMessage.has("type")) {
            logger.error("Message must contains type field: " + logMessage.toString());
            return;
        }
        String messageType = logMessage.get("type").getAsString();
        if (!MESSAGE_TYPES.containsKey(messageType)) {
            logger.error("Unknown message type: " + logMessage.toString());
            return;
        }
        Class messageClass = MESSAGE_TYPES.get(messageType);
        try {
            Object messageObj = messageClass.getConstructor(JsonObject.class).newInstance(logMessage);
            handleMessage(messageObj);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException | ClassCastException | IllegalStateException e) {
            logger.error("Failed to create message", e);
        }

    }


    private void handleMessage(Object messageObj) {
        if (messageObj instanceof BuildRequestMessage) {
            eventDispatcher.onBuildStart((BuildRequestMessage) messageObj);
        } else if (messageObj instanceof JobStartedMessage) {
            eventDispatcher.onJobStart((JobStartedMessage) messageObj);
        } else if (messageObj instanceof JobStoppedMessage) {
            eventDispatcher.onJobStop((JobStoppedMessage) messageObj);
        } else if (messageObj instanceof JobOutputMessage) {
            eventDispatcher.onJobOutput((JobOutputMessage) messageObj);
        } else if (messageObj instanceof WorkerStartedMessage) {
            eventDispatcher.onWorkerStart((WorkerStartedMessage) messageObj);
        } else if (messageObj instanceof WorkerStoppedMessage) {
            eventDispatcher.onWorkerStop((WorkerStoppedMessage) messageObj);
        } else if (messageObj instanceof WorkerInfoResponseMessage) {
            eventDispatcher.onWorkerInfo((WorkerInfoResponseMessage) messageObj);
        } else if(messageObj instanceof JobStopRequestMessage) {
            eventDispatcher.onJobStopRequest((JobStopRequestMessage) messageObj);
        }
    }

}
