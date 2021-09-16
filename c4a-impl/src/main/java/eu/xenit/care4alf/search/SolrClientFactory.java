package eu.xenit.care4alf.search;

import org.apache.commons.httpclient.HttpClient;

public interface SolrClientFactory {

    HttpClient getHttpClient();

}
