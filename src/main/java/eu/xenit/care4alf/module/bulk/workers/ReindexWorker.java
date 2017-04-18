package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;
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
                    solrAdmin.getSolrAdminClient().reindex(dbid);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
