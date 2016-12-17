package eu.xenit.care4alf.monitoring;

import org.antlr.misc.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@Component
public class GraphiteClient {
    private Logger logger = LoggerFactory.getLogger(GraphiteClient.class);


    private String graphiteHost;
    private int graphitePort;

    public GraphiteClient(){
        this("localhost", 2003);
    }

    public GraphiteClient(String graphiteHost, int graphitePort) {
        this.graphiteHost = graphiteHost;
        this.graphitePort = graphitePort;
    }

    public void send(Map<String,Long> metrics){
        try {
            Socket socket = new Socket(this.graphiteHost, this.graphitePort);
            OutputStream s = socket.getOutputStream();
            PrintWriter out = new PrintWriter(s, true);
            for (Map.Entry<String, Long> metric: metrics.entrySet()) {
                out.printf("%s %s %d%n", metric.getKey(), metric.getValue(), getDate());
            }
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            throw new GraphiteException("Unknown host: " + graphiteHost);
        } catch (IOException e) {
            throw new GraphiteException("Error while writing data to graphite: " + e.getMessage(), e);
        }
    }

    public void send(String key, long value){
        HashMap<String, Long> metrics = new HashMap<>();
        metrics.put(key, value);
        this.send(metrics);
    }

    private long getDate(){
        return Math.round(System.currentTimeMillis() / 1000.0d);
    }

}
