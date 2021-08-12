package eu.xenit.care4alf.search;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoPlatform;
import org.alfresco.httpclient.HttpClientFactory;
import org.apache.commons.httpclient.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@AlfrescoPlatform(minVersion="6.2")
@Component
public class SolrClientFactoryImpl implements SolrClientFactory {

    @Autowired
    @Qualifier("solrHttpClientFactory")
    private HttpClientFactory solrHttpClientFactory;

    @Override
    public HttpClient getHttpClient() {
        return solrHttpClientFactory.getHttpClient();
    }
}
