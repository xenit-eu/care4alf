package eu.xenit.care4alf.integrity;

import com.github.dynamicextensionsalfresco.actions.annotations.ActionMethod;
import com.github.dynamicextensionsalfresco.jobs.ScheduledQuartzJob;
import com.google.common.base.Optional;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.domain.qname.ibatis.QNameDAOImpl;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.alfresco.repo.solr.SOLRTrackingComponent.NodeQueryCallback;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Run at 3 am every saturday
@Component
@ScheduledQuartzJob(name = "IntegrityScan", group = "integrityscan", cron = "0 0 3 ? * SAT", cronProp = "c4a.integrity.cron")
public class IntegrityScanner implements Job {
    private Logger logger = LoggerFactory.getLogger(IntegrityScanner.class);

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
        return Optional.fromNullable(lastReport);
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        scanAll();
        // TODO send email
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
                verifyProperty(noderef, entry.getKey(), entry.getValue(), inProgressReport);
            }

            List<ChildAssociationRef> refList = nodeService.getParentAssocs(noderef);
            if (refList.isEmpty()) {
                inProgressReport.addNodeProblem(new IsolatedNodeProblem(noderef));
            }

            counter.incrementAndGet();
            inProgressReport.finish();
            return true;
        }
    }

    private void verifyProperty(NodeRef noderef, QName property, Serializable value, IntegrityReport report) {
        // Check for empty property/null property
        PropertyDefinition definition = dictionaryService.getProperty(property);
        if (definition == null) {
            logger.warn("Unknown property {} found on noderef {}", property, noderef);
            report.addNodeProblem(new UnknownPropertyProblem(noderef, property));
            return;
        }
        if (value == null) {
            if (definition.isMandatory()) {
                logger.error("{} is null for {}", property, noderef);
                if (ContentModel.PROP_HOMEFOLDER.equals(property)) {
                    Serializable username = nodeService.getProperty(noderef, ContentModel.PROP_USERNAME);
                    if (username.equals("mjackson") || username.equals("abeecher")) {
                        logger.info("Above node is {}, this is normal", username);
                    }
                }
                report.addNodeProblem(new MissingPropertyProblem(noderef, property));
            }
            return;
        }

        // Check for invalid deserialization
        DataTypeDefinition dataType = definition.getDataType();
        String className = getDeserializingClassName(dataType);
        try {
            Class clazz = Class.forName(className);
            if (!definition.isMultiValued()) {
                // single-valued test
                if (clazz.isInstance(value)) {
                    // bait out potential classCastException
                    clazz.cast(value);
                } else {
                    logger.error(value + " not instanceof " + className);
                    report.addNodeProblem(new IncorrectDataTypeProblem(noderef, property, dataType, className));
                }
            } else {
                // multi-valued test
                if (value instanceof Collection) {
                    // bait out potential classCastException
                    Iterator iterator = ((Collection) value).iterator();
                    if (iterator.hasNext()) {
                        clazz.cast(((Collection) value).iterator().next());
                    } else if (definition.isMandatory()) {
                        report.addNodeProblem(new MissingPropertyProblem(noderef, property));
                    }
                } else {
                    logger.error(value + " not instanceof Collection");
                    report.addNodeProblem(new IncorrectDataTypeProblem(noderef, property, dataType, className));
                }
            }
        } catch (ClassNotFoundException e) {
            report.addNodeProblem(new NondeserializableDataTypeProblem(noderef, property, className));
        } catch (ClassCastException e) {
            report.addNodeProblem(new IncorrectDataTypeProblem(noderef, property, dataType, className));
        }

    }

    private String getDeserializingClassName(DataTypeDefinition dataType) {
        /*  d:mltext is a generally awful-to-debug type. There is a Java class MLText in Alfresco,
         *  and if you ask the dataTypeDefinition what the associated Java class of d:mltext is,
         *  it will happily inform you that this is the MLText class. Yet if you have a d:mltext
         *  property and you get its value from the database, the Serializable you end up with
         *  will NOT be an instance of MLText. It will be an instance of String. _Which_ string
         *  that is depends on the locale of the user that executes the code.
         *
         *  So in here, we sidestep that entire issue by ignoring the datatype definition and
         *  pretending the associated class for d:mltext is simply String.
         */
        if (dataType.getName().getPrefixString().equals("d:mltext")) {
            return "java.lang.String";
        }
        return dataType.getJavaClassName();
    }
}
