package eu.xenit.care4alf.search;

import org.springframework.stereotype.Component;

/**
 * Created by willem on 5/16/17.
 */
@Component
public class Solr6AdminClientImpl extends Solr4AdminClientImpl{
    @Override
    protected String getSolrTypeUrl() {
        return "solr";
    }

}
