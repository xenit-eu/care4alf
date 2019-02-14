package eu.xenit.care4alf.integrity;

import com.github.dynamicextensionsalfresco.actions.annotations.ActionMethod;
import eu.xenit.care4alf.helpers.java8.Optional;
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
    private IntegrityReport lastReport;
    private IntegrityReport inProgressReport;

    @ActionMethod(SCAN_ALL_ACTION)
    public int scanAll() {
        counter = new AtomicInteger(0);

        NodeParameters nodeParameters = new NodeParameters();
        nodeParameters.setFromTxnId(0L);
        nodeParameters.setToTxnId(99999L);
        logger.debug("node params created");
        inProgressReport = new IntegrityReport();

        // This blocks until the callbackhandler has been called *and* returned for all discovered nodes
        solrTrackingComponent.getNodes(nodeParameters, new CallbackHandler());
        logger.debug("getNodes(…) executed, {} nodes", counter.get());
        inProgressReport.setScannedNodes(counter.get());
        lastReport = inProgressReport;
        return counter.get();
    }

    public Optional<IntegrityReport> getLastReport() {
        return Optional.ofNullable(lastReport);
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
                QName property = entry.getKey();
                if (entry.getValue() == null ) {
                    PropertyDefinition definition = dictionaryService.getProperty(property);
                    if (definition == null) {
                        logger.warn("Unknown property {} found on noderef {}", property, noderef);
                        inProgressReport.addNodeProblem(new UnknownPropertyProblem(noderef, property));
                    } else if (definition.isMandatory()) {
                        logger.error("{} is null for {}", property, noderef);
                        if (ContentModel.PROP_HOMEFOLDER.equals(property)) {
                            Serializable username = nodeService.getProperty(noderef, ContentModel.PROP_USERNAME);
                            if (username.equals("mjackson") ||username.equals("abeecher")) {
                                logger.info("Above node is {}, this is normal", username);
                            }
                        }
                        inProgressReport.addNodeProblem(new MissingPropertyProblem(noderef, property));
                    }
                }
            }
            counter.incrementAndGet();
            return true;
        }
    }
}
