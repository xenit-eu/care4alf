package eu.xenit.care4alf.integrity;

import com.github.dynamicextensionsalfresco.jobs.ScheduledQuartzJob;
import eu.xenit.care4alf.Config;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.domain.qname.ibatis.QNameDAOImpl;
import org.alfresco.repo.node.MLPropertyInterceptor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.alfresco.repo.solr.SOLRTrackingComponent.NodeQueryCallback;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ScheduledQuartzJob(name = "IntegrityScan", group = "integrityscan", cron = "* * * * * ? 2099", cronProp = "c4a.integrity.cron")
public class IntegrityScanner implements Job {
    private static final int BUFFER_SIZE = 8192;
    private Logger logger = LoggerFactory.getLogger(IntegrityScanner.class);

    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private ContentService contentService;
    @Autowired
    private SOLRTrackingComponent solrTrackingComponent;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private QNameDAOImpl qNameDAO;
    @Autowired
    private Config config;

    private AtomicInteger counter;
    private IntegrityReport lastReport;
    private IntegrityReport inProgressReport;

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

    public IntegrityReport getLastReport() {
        return lastReport;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        AuthenticationUtil.runAsSystem(new RunAsWork<Object>() {
            @Override
            public Object doWork() throws Exception {
                return IntegrityScanner.this.scanAll();
            }
        });
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
            // We'll have to work with multilang text objects in this transaction, rather than letting the interceptor
            // translate it to a string before giving it to us. We set this tx to 'ML aware', disabling the translation
            boolean wasMultiLangAware = MLPropertyInterceptor.isMLAware();
            MLPropertyInterceptor.setMLAware(true);
            try {
                Map<QName, Serializable> props = nodeService.getProperties(noderef);
                for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
                    verifyProperty(noderef, entry.getKey(), entry.getValue(), inProgressReport);
                }
            } finally {
                // Set mlaware back to what it was before we set it ourselves. Not 100% sure this is necessary.
                MLPropertyInterceptor.setMLAware(wasMultiLangAware);
            }

            List<ChildAssociationRef> refList = nodeService.getParentAssocs(noderef);
            if (refList.isEmpty()) {
                inProgressReport.addNodeProblem(new IsolatedNodeProblem(noderef));
            }

            verifyContentData(noderef, inProgressReport);

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
        String className = dataType.getJavaClassName();
        try {
            Class clazz = Class.forName(className);
            if (!definition.isMultiValued()) {
                // single-valued test
                if (clazz.isInstance(value)) {
                    // bait out potential classCastException
                    clazz.cast(value);
                } else {
                    logger.error("{}: Prop {} value {} not instance of {}", noderef, value, className);
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
                    logger.error("{}: multivalued prop {} value {} not instance of Collection", noderef, value);
                    report.addNodeProblem(new IncorrectDataTypeProblem(noderef, property, dataType, className));
                }
            }
        } catch (ClassNotFoundException e) {
            report.addNodeProblem(new NondeserializableDataTypeProblem(noderef, property, className));
            logger.error("{}: prop {} to be deserialized to unknown class {}", noderef, property, className);
        } catch (ClassCastException e) {
            report.addNodeProblem(new IncorrectDataTypeProblem(noderef, property, dataType, className));
            logger.error("{}: prop {} could not be cast to {}", noderef, property, className);
        }
    }

    private void verifyContentData(NodeRef noderef, IntegrityReport report) {
        ContentData contentData = (ContentData) nodeService.getProperty(noderef, ContentModel.PROP_CONTENT);
        if (contentData == null) {
            return;
        }
        verifyFilePresent(noderef, contentData, report);
        verifyEncoding(noderef, contentData, report);
    }

    private void verifyFilePresent(NodeRef noderef, ContentData contentData, IntegrityReport report) {
        String location = absolutePath(contentData);
        if (!Files.exists(Paths.get(location))) {
            logger.error("{} does not exist", location);
            report.addFileProblem(new FileNotFoundProblem(location, noderef));
        }
    }

    private void verifyEncoding(NodeRef noderef, ContentData contentData, IntegrityReport report) {
        String encoding = contentData.getEncoding();
        if (contentData.getMimetype().startsWith("text/") && encoding != null) {
            ContentReader reader = contentService.getRawReader(contentData.getContentUrl());
            if (reader.exists()) {
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                int hasRead = -1;
                try {
                    hasRead = reader.getReadableChannel().read(buffer);
                } catch (IOException e) {
                    report.addFileProblem(new FileEncodingProblem(absolutePath(contentData), encoding, noderef));
                }
                if (hasRead > 0) {
                    buffer.rewind();
                    CharsetDecoder decoder = Charset.forName(encoding).newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
                    CharBuffer uselessBuffer = CharBuffer.allocate(BUFFER_SIZE);
                    // Try decoding. If there were >8192 bytes in the file, the last bytes may form an invalid char
                    CoderResult res = decoder.decode(buffer, uselessBuffer, hasRead < BUFFER_SIZE);
                    if (res.isUnmappable() || res.isMalformed()) {
                        logger.warn("Can't decode {} as {}", noderef, encoding);
                        report.addFileProblem(new FileEncodingProblem(absolutePath(contentData), encoding, noderef));
                    }
                }
            }
        }
    }

    public String absolutePath(ContentData contentData) {
        String location = config.getProperty("dir.contentstore").replace("${dir.root}", config.getProperty("dir.root"));
        return contentData.getContentUrl().replace("store:/", location);
    }
}
