package simpleci.dispatcher.queue;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.message.LogMessage;

import java.io.IOException;
import java.util.Map;

public class ServiceMessageProducer {
    final Logger logger = LoggerFactory.getLogger(ServiceMessageProducer.class);
    private static final String EXCHANGE_NAME = "worker_service";

    private final Channel channel;
    private final Gson gson = new Gson();

    public ServiceMessageProducer(Connection connection) throws IOException {
        channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
    }

    public void send(LogMessage message) {
        String sendMessage = gson.toJson(message.toJson());
        try {
            channel.basicPublish(EXCHANGE_NAME, "", null, sendMessage.getBytes());
        } catch (IOException e) {
            logger.error("Error publish log message", e);
        }
    }
}
