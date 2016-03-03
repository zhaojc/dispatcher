package simpleci.dispatcher.listener.centrifugo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;

import java.util.Map;

public class CentrifugoApi {
    private final Jedis jedis;

    public CentrifugoApi() {
        this.jedis = new Jedis("localhost");
    }

    public void send(Object message, String channel) {
        Map sendData = new ImmutableMap.Builder<String, Object>()
                .put("data", new ImmutableList.Builder<Object>()
                        .add(
                                new ImmutableMap.Builder<String, Object>()
                                        .put("method", "publish")
                                        .put("params", new ImmutableMap.Builder<String, Object>()
                                                .put("channel", channel)
                                                .put("data", message)
                                                .build())
                                        .build())
                        .build())
                .build();

        Gson gson = new Gson();
        String sendMessage = gson.toJson(sendData);

        jedis.rpush("centrifugo.api", sendMessage);
    }
}
