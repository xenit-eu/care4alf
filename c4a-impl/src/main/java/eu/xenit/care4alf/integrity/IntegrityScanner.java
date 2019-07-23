package eu.xenit.care4alf.integrity;

import static org.alfresco.repo.action.executer.MailActionExecuter.*;

import com.github.dynamicextensionsalfresco.schedule.ScheduledTask;
import eu.xenit.care4alf.Config;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.StoreEntity;
import org.alfresco.repo.domain.qname.QNameDAO;
import org.alfresco.repo.domain.qname.ibatis.QNameDAOImpl;
import org.alfresco.repo.node.MLPropertyInterceptor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.alfresco.repo.solr.SOLRTrackingComponent.NodeQueryCallback;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
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
import org.apache.commons.validator.routines.EmailValidator;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
// !! Beware changing this group and name !! It's used in integrity.ts in the check in the callback of the REST call
@ScheduledTask(name = "IntegrityScan", group = "integrityscan", cron = "* * * * * ? 2099", cronProp = "c4a.integrity.cron")
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
    @Autowired
    private DataSource dataSource;
    @Autowired
    private ActionService actionService;

    private boolean shouldCancel;

    private AtomicInteger nodeCounter;
    private AtomicInteger fileCounter;
    private IntegrityReport lastReport;
    private Set<String> knownFileNames;

    public int scanAll() {
        shouldCancel = false;

        nodeCounter = new AtomicInteger(0);
        fileCounter = new AtomicInteger(0);
        knownFileNames = new HashSet<>();
        IntegrityReport inProgressReport = new IntegrityReport();

        NodeParameters nodeParameters = new NodeParameters();
        nodeParameters.setFromTxnId(0L);
        nodeParameters.setToTxnId(Long.MAX_VALUE);
        logger.debug("node params created");

        logger.info("Beginning Metadata Integrity Scan");
        // This blocks until the callbackhandler has been called *and* returned for all discovered nodes
        // OR until one of the calls to handleNode returns with false — which it does in case of a cancel
        solrTrackingComponent.getNodes(nodeParameters, new CallbackHandler(nodeCounter, knownFileNames, inProgressReport));
        if (shouldCancel) {
            return nodeCounter.get();
        }
        logger.info("Integrity Scan executed on {} nodes. Scanning filesystem next...", nodeCounter.get());
        inProgressReport.setScannedNodes(nodeCounter.get());

        try {
            // Scan all files in alf_data, see if we find any that didn't get turned up during our node scan
            verifyNoOrphans(knownFileNames, inProgressReport);
        } catch (IOException e) {
            inProgressReport.addFileProblem(new FileExceptionProblem(e));
        }

        // We may have returned from verifyNoOrphans due to a cancel
        // In that case we should not finish the report and return early
        if (shouldCancel) {
            return nodeCounter.get();
        }

        logger.info("Ended Metadata Integrity Scan");
        inProgressReport.finish();
        lastReport = inProgressReport;
        return nodeCounter.get();
    }

    public int getNodeProgress() {
        if (nodeCounter == null) {
            return -1;
        }
        return nodeCounter.get();
    }

    public int getFileProgress() {
        if (fileCounter == null) {
            return -1;
        }
        return fileCounter.get();
    }

    public IntegrityReport getLastReport() {
        return lastReport;
    }


    public IntegrityReport scanSubset(Iterator<NodeRef> nodeIter, Collection<String> fileCollection) {
        shouldCancel = false;

        logger.info("Starting scan of subset...");

        IntegrityReport subsetReport = new IntegrityReport();
        CallbackHandler handler = new CallbackHandler(new AtomicInteger(0), new HashSet<String>(), subsetReport);
        while (nodeIter.hasNext()) {
            // make a fake "node" out of the next noderef, pass it to the handler that does the actual verification
            // if this returns false, the operation has been cancelled
            boolean run = handler.handleNode(fakeNode(nodeIter.next()));
            if (!run) {
                return null;
            }
            subsetReport.setScannedNodes(subsetReport.getScannedNodes() + 1);
        }

        if (fileCollection.size() > 0) {
            final HashSet<String> files = new HashSet<>(fileCollection);
            new JdbcTemplate(this.dataSource).query("SELECT content_url FROM alf_content_url",
                    new OrphanCallbackHandler(files));
            for (String remaining : files) {
                subsetReport.addFileProblem(new OrphanFileProblem(absolutePath(remaining)));
            }
        }
        logger.info("Finished scan of subset");

        subsetReport.finish();
        return subsetReport;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        AuthenticationUtil.runAsSystem(new RunAsWork<Object>() {
            @Override
            public Object doWork() {
                return IntegrityScanner.this.scanAll();
            }
        });
        if (!shouldCancel) {
            mailReport(lastReport);
        }
    }

    public void cancelScan() {
        logger.warn("Cancelling integrity scan.");
        shouldCancel = true;
    }

    private class CallbackHandler implements NodeQueryCallback {
        private AtomicInteger nodeCounter;
        private Set<String> knownFileNames;
        private IntegrityReport inProgressReport;

        public CallbackHandler(AtomicInteger nodeCounter, Set<String> knownFileNames, IntegrityReport report) {
            this.inProgressReport = report;
            this.nodeCounter = nodeCounter;
            this.knownFileNames = knownFileNames;
        }

        @Override
        public boolean handleNode(Node node) {
            if (shouldCancel) {
                return false;
            }
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
            } catch (DataAccessException dae) {
                logger.warn("Could not get properties for {} from database, encountered {}", noderef,
                        dae.getClass().getSimpleName());
                inProgressReport.addNodeProblem(new NodeDataAccessProblem(noderef, "properties"));
            } catch (Exception e) {
                logger.error("Error {} when retrieving + verifying properties for node {}",
                        e.getClass().getSimpleName(), noderef);
                throw e;
            } finally {
                // Set mlaware back to what it was before we set it ourselves. Not 100% sure this is necessary.
                MLPropertyInterceptor.setMLAware(wasMultiLangAware);
            }

            try {
                List<ChildAssociationRef> refList = nodeService.getParentAssocs(noderef);
                // sys:store_root doesn't have a parent, this is normal and should not be reported
                if (refList.isEmpty() && !nodeService.getType(noderef).equals(ContentModel.TYPE_STOREROOT)) {
                    inProgressReport.addNodeProblem(new IsolatedNodeProblem(noderef));
                }
            } catch (DataAccessException dae) {
                logger.warn("Could not get parent assocs for {} from database, encountered {}", noderef,
                        dae.getClass().getSimpleName());
                inProgressReport.addNodeProblem(new NodeDataAccessProblem(noderef, "parent assocs"));
            }

            try {
                verifyContentData(noderef, inProgressReport, knownFileNames);
            } catch (DataAccessException dae) {
                logger.warn("Could not get ContentData property for {}, encountered {}", noderef,
                        dae.getClass().getSimpleName());
                inProgressReport.addNodeProblem(new NodeDataAccessProblem(noderef, "ContentData property"));
            }

            int count = nodeCounter.incrementAndGet();
            if (count % 10000 == 0) {
                logger.debug("Metadata Integrity Scan handled {} nodes so far", count);
            }
            return true;
        }
    }

    private void verifyProperty(NodeRef noderef, QName property, Serializable value, IntegrityReport report) {
        PropertyDefinition definition = dictionaryService.getProperty(property);
        // Check for empty property/null property
        if (value == null) {
            if (definition != null && definition.isMandatory()) {
                logger.error("{} is null for {}", property, noderef);
                String note = null; // if it stays null it won't be included by jackson when it serializes Problem
                if (ContentModel.PROP_HOMEFOLDER.equals(property)) {
                    Serializable username = nodeService.getProperty(noderef, ContentModel.PROP_USERNAME);
                    if (username.equals("mjackson") || username.equals("abeecher")) {
                        logger.info("Above node is {}, this is normal", username);
                        note = "This is one of the default users (" + username + "), in a default install they aren't"
                                + " given a homefolder (despite it being mandatory)";
                    }
                }
                MissingPropertyProblem problem = new MissingPropertyProblem(noderef, property);
                problem.setExtraMessage(note);
                report.addNodeProblem(problem);
            }
            return;
        }

        if (definition == null) {
            if (!property.toString().equals("{http://www.alfresco.org/model/content/1.0}authorizationStatus")) {
                // authorizationStatus isn't part of the content model, but is added on logged-in users
                // this is just an alfresco quirk and not "abnormal"
                logger.warn("Unknown property {} found on noderef {}", property, noderef);
                report.addNodeProblem(new UnknownPropertyProblem(noderef, property));
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
                    logger.error("{}: Prop {} value {} not instance of {}", noderef, property, value, className);
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
                    logger.error("{}: multival prop {} value {} not instance of Collection", noderef, property, value);
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

    private void verifyContentData(NodeRef noderef, IntegrityReport report, Set<String> fileNames) {
        ContentData contentData = (ContentData) nodeService.getProperty(noderef, ContentModel.PROP_CONTENT);
        if (contentData == null) {
            return;
        }
        verifyFilePresent(noderef, contentData, report);
        verifyEncoding(noderef, contentData, report);

        String contentUrl = contentData.getContentUrl();
        fileNames.add(fileNameFromPath(contentUrl));
    }

    private void verifyFilePresent(NodeRef noderef, ContentData contentData, IntegrityReport report) {
        String location = absolutePath(contentData);
        if (!Files.exists(Paths.get(location))) {
            logger.error("{} does not exist ({})", location, noderef);
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

    private void verifyNoOrphans(final Set<String> known, final IntegrityReport report) throws IOException {
        // This function finds orphaned files in alf_data, i.e. files that have no trace in the db of why they're there.
        // `Set<String> known` is a set of filenames (<guid>.bin) that we found when scanning through the nodes.
        // Any files we find in alf_data that aren't in this set are candidates for being an orphan.
        // They can also be nodes that have been deleted recently, or a file that's referenced via a property other than
        // cm:contentData:contentUrl
        final Set<String> potentialOrphans = new HashSet<>();
        Files.walkFileTree(Paths.get(getContentStoreDir()), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
                if (shouldCancel) {
                    return FileVisitResult.TERMINATE;
                }
                if (attr.isRegularFile() && !known.contains(file.getFileName().toString())) {
                    // We don't know if it's a problem yet, might be a recently deleted file
                    // Investigate this one further by looking in the db (also convert path to store://, like db uses)
                    potentialOrphans.add(relativePath(file.toString()));
                }
                int count = fileCounter.incrementAndGet();
                if (count % 10000 == 0) {
                    logger.debug("Scanned {} files so far", count);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        if (shouldCancel) {
            return;
        }

        // Query the db to see if our potential orphans are in there. If not, they're a genuine orphan and should be
        // reported in the scan.
        new JdbcTemplate(this.dataSource).query("SELECT content_url FROM alf_content_url",
                new OrphanCallbackHandler(potentialOrphans));

        for (String remaining : potentialOrphans) {
            report.addFileProblem(new OrphanFileProblem(absolutePath(remaining)));
        }
    }

    private void mailReport(IntegrityReport report) {
        IntegrityReportSummary summary = new IntegrityReportSummary(report);
        String recipientString = config.getProperty("c4a.integrity.recipients");
        if (recipientString == null || recipientString.equals("")) {
            logger.info("No recipients configured for integrity report (c4a.integrity.recipients)");
            return;
        }
        // If you feel like being pedantic, this parsing of email addresses is insufficient because commas can be valid
        // in an email address, if quoted. E.g. "john,doe"@example.com is a syntactically valid email address.
        // But if we're being realistic, no one uses quoted email addresses, and I don't feel like writing a parser.
        String[] recipientsArray = config.getProperty("c4a.integrity.recipients").split(",");
        // List isn't serializable, but the MailActionExecutor tries casting to List<String> anyway. So, ArrayList...?
        ArrayList<String> recipients = new ArrayList<>();
        for (String recipient : recipientsArray) {
            recipient = recipient.trim();
            if (EmailValidator.getInstance(true).isValid(recipient)) {
                recipients.add(recipient);
                logger.info("Sending integrity report email to {}", recipient);
            } else {
                logger.error("Configured as recipient but invalid email address: '{}'", recipient);
            }
        }
        Action mail = actionService.createAction(NAME);
        mail.setParameterValue(PARAM_SUBJECT, "Alfresco Metadata Integrity Report");
        mail.setParameterValue(PARAM_FROM, config.getProperty("c4a.integrity.mailfrom", "noreply@localhost"));
        mail.setParameterValue(PARAM_TO_MANY, recipients);
        mail.setParameterValue(PARAM_TEXT, summary.toString());
        actionService.executeAction(mail, null);
    }

    private Node fakeNode(final NodeRef noderef) {
        // Create an anonymous subclass of NodeEntity that contains our node
        // Basically a fake way to give nodes that need to be converted to noderefs anyway, because that's what the
        // CallbackHandler requires
        return new NodeEntity() {
            @Override
            public boolean getDeleted(QNameDAO dao) {
                return false;
            }

            @Override
            public StoreEntity getStore() {
                final StoreEntity storeEntity = new StoreEntity();
                storeEntity.setProtocol(noderef.getStoreRef().getProtocol());
                storeEntity.setIdentifier(noderef.getStoreRef().getIdentifier());
                return storeEntity;
            }

            @Override
            public NodeRef getNodeRef() {
                return noderef;
            }
        };
    }

    public String absolutePath(ContentData contentData) {
        return absolutePath(contentData.getContentUrl());
    }

    public String absolutePath(String path) {
        return path.replace("store:/", getContentStoreDir());
    }

    public String relativePath(String path) {
        return path.replace(getContentStoreDir(), "store:/");
    }

    public String fileNameFromPath(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public String getContentStoreDir() {
        return config.getFullyParsedProperty("dir.contentstore");
    }
}
