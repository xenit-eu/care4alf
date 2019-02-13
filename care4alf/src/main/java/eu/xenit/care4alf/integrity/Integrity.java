package eu.xenit.care4alf.integrity;

import com.github.dynamicextensionsalfresco.actions.annotations.ActionMethod;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.domain.qname.ibatis.QNameDAOImpl;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.alfresco.repo.solr.SOLRTrackingComponent.NodeQueryCallback;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Integrity {
    private Logger logger = LoggerFactory.getLogger(Integrity.class);

    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private SOLRTrackingComponent solrTrackingComponent;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private QNameDAOImpl qNameDAO;

    private static final String SCAN_ALL_ACTION = "scan-all";
    private AtomicInteger counter;

    @ActionMethod(SCAN_ALL_ACTION)
    public int scanAll() throws InterruptedException {

        counter = new AtomicInteger(0);

        NodeParameters nodeParameters = new NodeParameters();
        nodeParameters.setFromTxnId(0L);
        nodeParameters.setToTxnId(99999L);
        logger.debug("node params created");
        // This blocks until the callbackhandler has been called *and* returned for all discovered nodes
        solrTrackingComponent.getNodes(nodeParameters, new CallbackHandler());
        logger.debug("getNodes(…) executed, {} nodes", counter.get());
        Thread.sleep(1000);
        logger.debug("... {} nodes", counter.get());
        return counter.get();
    }

    public void startScan() {
        throw new NotImplementedException();
    }

    public void busyWait() {
        throw new NotImplementedException();
    }

    public IntegrityReport getLastReport() {
        throw new NotImplementedException();
    }

    private class CallbackHandler implements NodeQueryCallback {
        @Override
        public boolean handleNode(Node node) {
            if (node.getDeleted(qNameDAO)
                    || !node.getStore().getStoreRef().equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE)) {
                return true; // continue to next node
            }

            NodeRef noderef = node.getNodeRef();
            Map<QName, Serializable> props = nodeService.getProperties(noderef);
            for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
                if (entry.getValue() == null ) {
                    PropertyDefinition definition = dictionaryService.getProperty(entry.getKey());
                    if (definition == null) {
                        logger.warn("Unknown property {} found on noderef {}", entry.getKey(), noderef);
                    } else if (definition.isMandatory()) {
                        logger.error("{} is null for {}", entry.getKey(), noderef);
                        if (ContentModel.PROP_HOMEFOLDER.equals(entry.getKey())) {
                            Serializable username = nodeService.getProperty(noderef, ContentModel.PROP_USERNAME);
                            if (username.equals("mjackson") ||username.equals("abeecher")) {
                                logger.info("Above node is {}, this is normal", username);
                            }
                        } else {
                            logger.info("{} doesn't equal {}", entry.getKey(), ContentModel.PROP_HOMEFOLDER);
                        }
                    }
                }
            }
            counter.incrementAndGet();
            return true;
        }
    }
}
