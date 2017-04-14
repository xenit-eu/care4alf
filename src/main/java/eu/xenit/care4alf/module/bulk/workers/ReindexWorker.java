package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.Config;
import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import eu.xenit.care4alf.search.AbstractSolrAdminClient;
import eu.xenit.care4alf.search.Solr1AdminClientImpl;
import eu.xenit.care4alf.search.Solr4AdminClientImpl;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Thomas.Straetmans on 14/04/2017.
 */

@Component
@Worker(action = "reindex")
public class ReindexWorker extends AbstractWorker{

    public ReindexWorker() {
        super(null);
    }

    public ReindexWorker(JSONObject parameters)
    {
        super(parameters);
    }

    @Override
    public void process(NodeRef entry) throws Throwable {
        String obj = (String) this.nodeService.getProperty(entry, ContentModel.PROP_NODE_DBID);
        if(obj != "" && obj != null) {
            Long dbid = Long.decode(obj);
                    getSolrAdminClient().reindex(dbid);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Autowired
    Solr4AdminClientImpl solr4AdminClient;

    @Autowired
    Solr1AdminClientImpl solr1AdminClient;

    @Autowired
    Config config;

    private AbstractSolrAdminClient getSolrAdminClient() {
        String searchSubSystem = getSearchSubSystemName();
        if(searchSubSystem.equals("solr4"))
            return solr4AdminClient;
        return solr1AdminClient;
    }

    private String getSearchSubSystemName(){
        return config.getProperty("index.subsystem.name");
    }
}
