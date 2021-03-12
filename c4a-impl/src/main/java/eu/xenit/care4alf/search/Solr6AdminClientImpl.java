package eu.xenit.care4alf.search;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 5/16/17.
 */
@Component
@Conditional(SolrSubsystemConditions.Solr6Condition.class)
public class Solr6AdminClientImpl extends Solr4AdminClientImpl{
    @Override
    protected String getSolrTypeUrl() {
        return "solr";
    }

}
