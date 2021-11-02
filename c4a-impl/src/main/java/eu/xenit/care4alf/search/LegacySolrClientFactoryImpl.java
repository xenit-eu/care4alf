package eu.xenit.care4alf.search;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoPlatform;
import eu.xenit.care4alf.Config;
import org.alfresco.httpclient.HttpClientFactory;
import org.alfresco.repo.search.impl.solr.SolrChildApplicationContextFactory;
import org.apache.commons.httpclient.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@AlfrescoPlatform(maxVersion="6.1.1")
public class LegacySolrClientFactoryImpl implements SolrClientFactory {

    private final static Logger logger = LoggerFactory.getLogger(LegacySolrClientFactoryImpl.class);

    @Autowired
    private Config config;

    @Autowired(required = false)
    @Qualifier("solr")
    SolrChildApplicationContextFactory solr1HttpClientFactory;

    @Autowired(required = false)
    @Qualifier("solr4")
    SolrChildApplicationContextFactory solr4HttpClientFactory;

    @Override
    public HttpClient getHttpClient() {
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
}
