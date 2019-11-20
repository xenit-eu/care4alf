package eu.xenit.care4alf.search;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import eu.xenit.care4alf.Config;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Component
public class SolrClientImpl implements SolrClient {

    private final static Logger logger = LoggerFactory.getLogger(SolrClientImpl.class);

    @Autowired
    private Config config;

    @Autowired(required = false)
    @Qualifier("solr")
    SolrChildApplicationContextFactory solr1HttpClientFactory;

    @Autowired(required = false)
    @Qualifier("solr4")
    SolrChildApplicationContextFactory solr4HttpClientFactory;

    private HttpClient getHttpClient() {
        String searchSubsystemKey = "index.subsystem.name";
        String searchSubSystemValue = config.getProperty(searchSubsystemKey).toLowerCase();

        // Default factory is Solr (meaning Solr 1), which means it is also used when 'solr6' is configured.
        SolrChildApplicationContextFactory clientFactory = solr1HttpClientFactory;
        if (searchSubSystemValue.equals("solr4")) {
            clientFactory = solr4HttpClientFactory;
        }
        logger.info("Configured '{}'='{}' -> solrHttpClientFactory.typeName='{}'.",
                searchSubsystemKey, searchSubSystemValue, clientFactory.getTypeName());


        Object httpClientFactory = clientFactory.getApplicationContext().getBean("solrHttpClientFactory");
        return ((HttpClientFactory) httpClientFactory).getHttpClient();
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
        post.setRequestEntity(new ByteArrayRequestEntity(body.toString().getBytes("UTF-8"),
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

}