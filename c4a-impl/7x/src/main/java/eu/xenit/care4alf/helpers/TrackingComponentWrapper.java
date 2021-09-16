package eu.xenit.care4alf.helpers;

import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.search.SearchTrackingComponent.NodeQueryCallback;
import org.alfresco.repo.solr.NodeParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TrackingComponentWrapper {

    @Autowired
    SearchTrackingComponent searchTrackingComponent;

    public void getNodes(NodeParameters nodeParameters, NodeQueryCallback callback) {
        searchTrackingComponent.getNodes(nodeParameters, callback);
    }

    public interface NodeQueryCallbackWrapper extends NodeQueryCallback {}

}
