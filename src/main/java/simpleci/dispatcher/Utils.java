package simpleci.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private Utils(){

    }

    public static boolean waitForPort(String host, int port, int numberOfAttemps, int attemptDelay ) {
        for(int attempt = 1; attempt <= numberOfAttemps; attempt++) {
            logger.info(String.format("Connecting to %s:%d, attempt %d", host, port, attempt));
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 500);
                return true;
            } catch (IOException e) {
                try {
                    Thread.sleep(attemptDelay);
                } catch (InterruptedException e1) {

                }
            }
        }
        return false;
    }
}
