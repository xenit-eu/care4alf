package eu.xenit.care4alf.search;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.health.Service;
import eu.xenit.care4alf.Config;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.alfresco.httpclient.HttpClientFactory;
import org.alfresco.repo.search.impl.solr.SolrChildApplicationContextFactory;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Component
public class SolrClientImpl implements SolrClient {

    private final static Logger logger = LoggerFactory.getLogger(SolrClientImpl.class);

    private static final int SOLRDEFAULTPORT = 8080;
    private static final String DNS_RESOLUTION_STRATEGY_SYSTEM = "SYSTEM";
    private static final String DNS_RESOLUTION_STRATEGY_CONSUL = "CONSUL";
    private static final String DNS_RESOLUTION_STRATEGY_HAPROXY = "HAPROXY";
    private static final String DNS_RESOLUTION_STRATEGY_DIRECT = "DIRECT";
    // NOTE socket command needs to be terminated correctly with a `\n`
    private static final String HAPROXY_SOCKETCMD_SHOWSTAT = "show stat\n";

    @Autowired
    private Config config;

    @Autowired(required = false)
    @Qualifier("solr")
    SolrChildApplicationContextFactory solr1HttpClientFactory;

    @Autowired(required = false)
    @Qualifier("solr4")
    SolrChildApplicationContextFactory solr4HttpClientFactory;

    String targetHost;
    int targetPort;
    Map<String, Integer> availableHosts;

    private HttpClientFactory getHttpClientFactory() {
        String searchSubsystemKey = "index.subsystem.name";
        String searchSubSystemValue = config.getProperty(searchSubsystemKey).toLowerCase();

        // Default factory is Solr (meaning Solr 1), which means it is also used when 'solr6' is configured.
        SolrChildApplicationContextFactory clientFactory = solr1HttpClientFactory;
        if (searchSubSystemValue.equals("solr4")) {
            clientFactory = solr4HttpClientFactory;
        }
        logger.info("Configured '{}'='{}' -> solrHttpClientFactory.typeName='{}'.",
                searchSubsystemKey, searchSubSystemValue, clientFactory.getTypeName());

        return (HttpClientFactory) clientFactory.getApplicationContext().getBean("solrHttpClientFactory");
    }

    private HttpClient getHttpClient() {
        if (Boolean.parseBoolean(config.getProperty("xenit.care4alf.solr.loadbalanced"))) {
            return getHttpClientFactory().getHttpClient(targetHost, targetPort);
        } else {
            return getHttpClientFactory().getHttpClient();
        }
    }

    @Override
    public JSONObject postJSON(String url, Multimap<String, String> parameters, JSONObject body)
            throws IOException, EncoderException, JSONException {
        try {
            return new JSONObject(basePost(url, parameters, body == null ? null : body.toString()));
        } catch (ConnectException ce) {
            logger.error("Solr connection issues. Please check Solr is started and connected correctly");
            return new JSONObject("{\"Summary\": {\"Error\": -2}}");
        }
    }

    @Override
    public String postMessage(String url, Multimap<String, String> parameters, String body)
            throws IOException, EncoderException {
        try {
            return basePost(url, parameters, body);
        } catch (ConnectException ce) {
            logger.error("Solr connection issues. Please check Solr is started and connected correctly");
            return "No Solr connected";
        }
    }

    @Override
    public String get(String url, Multimap<String, String> parameters) throws IOException, EncoderException {
        try {
            return baseGET(url, parameters);
        } catch (ConnectException ce) {
            logger.error("Solr connection issues. Please check Solr is started and connected correctly");
            return "No Solr connected";
        }
    }

    private String basePost(String url, Multimap<String, String> parameters, String body)
            throws IOException, EncoderException {
        final HttpClient httpClient = getHttpClient();
        HttpClientParams params = httpClient.getParams();
        params.setBooleanParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, true);
        httpClient.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials("admin", "admin"));

        final URLCodec encoder = new URLCodec();
        StringBuilder urlBuilder = new StringBuilder();
        if (parameters == null) {
            parameters = ArrayListMultimap.create();
        }

        for (Map.Entry<String, String> entry : parameters.entries()) {
            if (urlBuilder.length() == 0) {
                urlBuilder.append("?");
            } else {
                urlBuilder.append("&");
            }
            String value = entry.getValue();
            logger.debug("Key/Value {}={}", entry.getKey(), entry.getValue());
            if (value.indexOf('+') == -1) {
                value = encoder.encode(value, "UTF-8");
            }
            urlBuilder.append(encoder.encode(entry.getKey())).append("=").append(value);
        }
        urlBuilder.insert(0, url);
        logger.info("parameters {}", parameters);

        final String uri = urlBuilder.toString();
        logger.info("solr query: {}", uri);

        PostMethod post = new PostMethod(uri);
        if (body == null) {
            body = "{}";
        }
        post.setRequestEntity(new ByteArrayRequestEntity(body.getBytes(StandardCharsets.UTF_8),
                body.startsWith("{") ? "application/json" : "text/xml"));

        try {
            httpClient.executeMethod(post);

            if (post.getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY
                    || post.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                Header locationHeader = post.getResponseHeader("location");
                if (locationHeader != null) {
                    String redirectLocation = locationHeader.getValue();
                    post.setURI(new URI(redirectLocation, true));
                    httpClient.executeMethod(post);
                }
            }

            if (post.getStatusCode() != HttpServletResponse.SC_OK) {
                logger.error("HTTP error: " + post.getResponseBodyAsString());
                throw new IOException("Request failed " + post.getStatusCode());
            }

            return post.getResponseBodyAsString();
        } finally {
            post.releaseConnection();
        }
    }

    private String baseGET(String url, Multimap<String, String> parameters) throws IOException, EncoderException {
        final HttpClient httpClient = getHttpClient();
        HttpClientParams params = httpClient.getParams();
        params.setBooleanParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, true);
        httpClient.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials("admin", "admin"));

        final URLCodec encoder = new URLCodec();
        StringBuilder urlBuilder = new StringBuilder();
        if (parameters == null) {
            parameters = ArrayListMultimap.create();
        }

        for (Map.Entry<String, String> entry : parameters.entries()) {
            if (urlBuilder.length() == 0) {
                urlBuilder.append("?");
            } else {
                urlBuilder.append("&");
            }
            String value = entry.getValue();
            logger.debug("Key/Value {}={}", entry.getKey(), entry.getValue());
            if (value.indexOf('+') == -1) {
                value = encoder.encode(value, "UTF-8");
            }
            urlBuilder.append(encoder.encode(entry.getKey())).append("=").append(value);
        }
        urlBuilder.insert(0, url);
        logger.info("parameters {}", parameters);

        final String uri = urlBuilder.toString();
        logger.info("solr query: {}", uri);

        GetMethod get = new GetMethod(uri);

        try {
            httpClient.executeMethod(get);

            if (get.getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY
                    || get.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                Header locationHeader = get.getResponseHeader("location");
                if (locationHeader != null) {
                    String redirectLocation = locationHeader.getValue();
                    get.setURI(new URI(redirectLocation, true));
                    httpClient.executeMethod(get);
                }
            }

            if (get.getStatusCode() != HttpServletResponse.SC_OK) {
                logger.error("HTTP error: " + get.getResponseBodyAsString());
                throw new IOException("Request failed " + get.getStatusCode());
            }

            return get.getResponseBodyAsString();
        } finally {
            get.releaseConnection();
        }
    }

    private void queryForAvailableHosts() throws IOException, ExecutionException, InterruptedException {
        String dnsResolutionStrategy = config.getProperty("xenit.care4alf.solr.dnsresolutionstrategy", DNS_RESOLUTION_STRATEGY_SYSTEM);
        Map<String, Integer> solrIpAdresses = null;
        switch(dnsResolutionStrategy) {
            case DNS_RESOLUTION_STRATEGY_CONSUL:
                solrIpAdresses = queryConsulForHosts();
                break;
            case DNS_RESOLUTION_STRATEGY_HAPROXY:
                solrIpAdresses = queryHAproxyForHosts();
                break;
            //TODO switch DIRECT and SYSTEM for backwards compatibility?
            case DNS_RESOLUTION_STRATEGY_DIRECT:
                solrIpAdresses = directResolution();
                break;
            case DNS_RESOLUTION_STRATEGY_SYSTEM:
            default:
                solrIpAdresses = querySystemForHosts();
        }

    }

    private Map<String, Integer> directResolution(){
        HashMap<String, Integer> response = new HashMap<>();
        response.put(config.getProperty("solr.host"), Integer.parseInt(config.getProperty("solr.port")));
        return response;
    }

    private Map<String, Integer> queryHAproxyForHosts() throws IOException, ExecutionException, InterruptedException {
        SocketAddress socketAddress= new AFUNIXSocketAddress(new File(config.getProperty("xenit.care4alf.solr.proxy.socket")));
        try (AFUNIXSocket socket = AFUNIXSocket.newInstance()) {
            socket.connect(socketAddress);
            logger.debug("connected to socket {}", socketAddress);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> socketResponseAsFuture = executor.submit(new UnixSocketRead(socket));
            OutputStream socketOutputStream = socket.getOutputStream();
            socketOutputStream.write(HAPROXY_SOCKETCMD_SHOWSTAT.getBytes());
            socketOutputStream.flush();
            String rawSocketResponse = socketResponseAsFuture.get();
            return parseRawSocketResponse(rawSocketResponse);
        }
    }

    private Map<String, Integer> parseRawSocketResponse(String rawResponse) {
        return null;
    }

    private Map<String, Integer> queryConsulForHosts() {
        Consul baseClient = Consul.builder().build();
        AgentClient agentClient = baseClient.agentClient();
        return agentClient.getServices()
                .entrySet().stream()
                .filter(this::isSolrService).filter(this::isProjectService)
                .map(Entry::getValue)
                .collect(Collectors.toMap(Service::getAddress, Service::getPort));
    }

    private Map<String, Integer> querySystemForHosts() {
        String hostName = config.getProperty("xenit.solr.hostname", "solr");
        try {
            Map<String, Integer> hostsmap = Arrays.stream(InetAddress.getAllByName(hostName))
                    .collect(Collectors.toMap(InetAddress::getHostAddress, inetAddress -> SOLRDEFAULTPORT));
        } catch (UnknownHostException e) {
            logger.warn("Cannot find hosts by name {}", hostName);
        }
        return new HashMap<>();
    }

    private boolean isSolrService(Entry<String, Service> serviceEntry) {
        return serviceEntry.getKey().contains("solr");
    }

    private boolean isProjectService(Entry<String, Service> serviceEntry) {
        return serviceEntry.getKey().contains(config.getProperty("xenit.projectname", ""));
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    protected class UnixSocketRead implements Callable<String> {

        private StringBuffer stringBuffer;
        private AFUNIXSocket socket;

        public UnixSocketRead(AFUNIXSocket connectedSocket) {
            this.stringBuffer = new StringBuffer();
            this.socket = connectedSocket;
        }

        @Override
        public String call() throws Exception {
            int readBytes;
            byte[] byteBuffer = new byte[socket.getReceiveBufferSize()];
            InputStream socketInputStream = socket.getInputStream();
            while ((readBytes = socketInputStream.read(byteBuffer)) != -1) {
                stringBuffer.append(new String(byteBuffer));
            }
            return stringBuffer.toString();
        }
    }
}
