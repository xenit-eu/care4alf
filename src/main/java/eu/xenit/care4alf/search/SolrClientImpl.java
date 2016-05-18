package eu.xenit.care4alf.search;

import com.google.common.collect.Multimap;
import org.alfresco.httpclient.HttpClientFactory;
import org.alfresco.repo.search.impl.solr.SolrChildApplicationContextFactory;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author Laurent Van der Linden
 */
@Component
public class SolrClientImpl implements SolrClient {
    private final static Logger logger = LoggerFactory.getLogger(SolrClientImpl.class);

    @Autowired()
    @Resource(name = "solr")
    SolrChildApplicationContextFactory solrhttpClientFactory;

    @Override
    public JSONObject post(String url, Multimap<String, String> parameters, JSONObject body) throws IOException, EncoderException, JSONException
    {
        HttpClientFactory httpClientFactory = (HttpClientFactory) (solrhttpClientFactory).getApplicationContext().getBean("solrHttpClientFactory");
        final HttpClient httpClient = httpClientFactory.getHttpClient();
        HttpClientParams params = httpClient.getParams();
        params.setBooleanParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, true);
        httpClient.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials("admin", "admin"));

        final URLCodec encoder = new URLCodec();
        StringBuilder urlBuilder = new StringBuilder();
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
        if (body != null) {
            post.setRequestEntity(new ByteArrayRequestEntity(body.toString().getBytes("UTF-8"), "application/json"));
        }

        try {
            httpClient.executeMethod(post);

            if (post.getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY || post.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
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

            final String responseBodyAsString = post.getResponseBodyAsString();
            return new JSONObject(responseBodyAsString);
        } finally {
            post.releaseConnection();
        }
    }

    @Override
    public JSONObject post(String url, Multimap<String, String> parameters) throws IOException, EncoderException, JSONException {
        return post(url, parameters, null);
    }
}
