package eu.xenit.care4alf.helpers;

import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TrackingComponentWrapper {

    @Autowired
    SOLRTrackingComponent solrTrackingComponent;

    public void getNodes(NodeParameters nodeParameters, SOLRTrackingComponent.NodeQueryCallback callback){
        solrTrackingComponent.getNodes(nodeParameters, callback);
    }
    public interface NodeQueryCallbackWrapper extends SOLRTrackingComponent.NodeQueryCallback {}

}
