package eu.xenit.care4alf.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import eu.xenit.care4alf.Config;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import javax.sql.DataSource;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.codec.EncoderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;


@Component
@WebScript(baseUri = "/xenit/care4alf/solr", families = {"care4alf"}, description = "Solr administration")
@Authentication(AuthenticationType.ADMIN)
public class SolrAdmin {

    private final Logger logger = LoggerFactory.getLogger(SolrAdmin.class);

    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private NodeService nodeService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    AbstractSolrAdminClient solrAdminClient;

    public AbstractSolrAdminClient getSolrAdminClient() {
        return solrAdminClient;
    }

    @Uri("errors")
    public void errors(final WebScriptResponse response, @RequestParam(defaultValue = "0") String start,
            @RequestParam(defaultValue = "100") String rows) throws EncoderException, IOException {
        JsonNode json = this.getSolrAdminClient().getSolrErrorsJson(Integer.parseInt(start), Integer.parseInt(rows));
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }

    public JsonNode getSolrSummaryJson() throws EncoderException, IOException {
        return this.getSolrAdminClient().getSolrSummaryJson();
    }

    public long getSolrErrors() {
        try {
            JsonNode json = this.getSolrAdminClient().getSolrErrorsJson(0, 0);
            System.out.println(json.toString());
            JsonNode numFoundNode = json.get("response").get("numFound");
            return numFoundNode.canConvertToLong() ? numFoundNode.asLong() : -1L;
        } catch (NullPointerException | EncoderException | IOException e) {
            return -1;
        }
    }

    @Uri("proxy/{uri}")
    public void proxy(final WebScriptRequest request, final WebScriptResponse response, @UriVariable("uri") String uri)
            throws EncoderException, IOException {
        String[] names = request.getParameterNames();
        Multimap<String, String> parameters = ArrayListMultimap.create();
        for (String name : names) {
            parameters.put(name, request.getParameter(name));
        }
        String result = this.getSolrAdminClient().getSolrClient()
                .get("/" + this.getSolrAdminClient().getSolrTypeUrl() + "/" + uri, parameters);
        response.setContentType("application/json");
        response.getWriter().write(result);
    }

    @Uri("errors/nodes")
    public void getRESTSolrErrorNodes(final WebScriptResponse response, @RequestParam(defaultValue = "100") int rows)
            throws IOException, EncoderException {
        response.getWriter().write(this.getSolrErrorNodes(rows));
    }

    @Uri(value = "errors/nodes/fix/{filter}", method = HttpMethod.POST)
    public void fixSolrErrors(
            final WebScriptResponse res,
            @UriVariable() String filter,
            @RequestParam(defaultValue = "10000") final int rows,
            @RequestParam(defaultValue = "nocontent") final String action)
            throws IOException, EncoderException {
        List<SolrErrorDoc> docs = this.getSolrAdminClient().getSolrErrorDocs(rows);
        int count = 0;
        if (filter == null) {
            filter = "all";
        }
        logger.debug("filter: '{}'", filter);
        for (SolrErrorDoc doc : docs) {
            logger.debug("Processing document with id={}, Exception: '{}', Action={}", doc.getDbid(),
                    doc.getException(), action);
            if ((filter.equals("all") || doc.getException().equals(filter))) {
                if (action.equals("nocontentindexing")) {
                    logger.debug("Setting content-indexing to false");
                    NodeRef nodeRef = nodeService.getNodeRef(doc.getDbid());
                    nodeService.setProperty(nodeRef, ContentModel.PROP_IS_CONTENT_INDEXED, false);
                    count++;
                } else if (action.equals("reindex")) {
                    logger.debug("Reindexing node");
                    this.getSolrAdminClient().reindex(doc.getDbid());
                    count++;
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    logger.debug("Ignoring document");
                }
            }
        }
        res.getWriter().write(Integer.toString(count));
    }

    public String getSolrErrorNodes(int rows) throws EncoderException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("exception;txid;dbid;noderef;type;name;created;modified;filesize\n");
        List<SolrErrorDoc> docs = this.getSolrAdminClient().getSolrErrorDocs(rows);
        for (SolrErrorDoc doc : docs) {
            NodeRef nodeRef = this.nodeService.getNodeRef(doc.getDbid());
            ContentData content = (ContentData) this.nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
            long size = -1;
            if (content != null) {
                size = content.getSize();
            }
            String[] fields = new String[]{
                    doc.getException(),
                    Long.toString(doc.getTxid()),
                    Long.toString(doc.getDbid()),
                    orEmpty(nodeRef),
                    nodeRef == null ? "" : this.nodeService.getType(nodeRef).toString(),
                    orEmpty(this.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME)),
                    orEmpty(this.nodeService.getProperty(nodeRef, ContentModel.PROP_CREATED)),
                    orEmpty(this.nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED)),
                    Long.toString(size)
            };
            for (String field : fields) {
                sb.append(field).append(",");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String orEmpty(Object o) {
        return o == null ? "" : o.toString();
    }


    public long getSolrLag() {
        JsonNode summary = null;
        try {
            summary = this.getSolrAdminClient().getSolrSummaryJson();
            String lag = summary.get("alfresco").get("TX Lag").asText();
            return Long.parseLong(lag.replace(" s", ""));
        } catch (NullPointerException | EncoderException | IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Uri("optimize")
    public void optimize(WebScriptResponse res) throws IOException, EncoderException {
        res.getWriter().write(this.optimize());
    }

    public String optimize() throws IOException, EncoderException {
        return this.getSolrAdminClient().optimize();
    }

    @Uri("transactions")
    public void getTransactionsToIndex(WebScriptResponse response, @RequestParam(required = false) Long txId)
            throws IOException {
        new ObjectMapper().writeValue(response.getWriter(),
                this.getTransactionsToIndex(txId == null ? this.geLastTxInIndex() : txId));
    }

    public List<Transaction> getTransactionsToIndex(long fromTxId) {
        return new JdbcTemplate(dataSource).query(
                "select TRANSACTION_ID, count(*) as n from alf_node where transaction_id >= " + Long.toString(fromTxId)
                        + " group by TRANSACTION_ID order by n desc",
                new Object[]{},
                new RowMapper<Transaction>() {
                    @Override
                    public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new Transaction(rs.getLong(1), rs.getInt(2));
                    }
                });
    }

    private long geLastTxInIndex() {
        JsonNode summary = null;
        try {
            summary = this.getSolrAdminClient().getSolrSummaryJson();
            return summary.get("alfresco").get("Id for last TX in index").asLong(Long.MAX_VALUE);
        } catch (NullPointerException | EncoderException | IOException e) {
            e.printStackTrace();
        }
        return Long.MAX_VALUE;
    }

    public long getNodesToIndex() {
        Long result = new JdbcTemplate(dataSource)
                .queryForObject("select count(*) as n from alf_node where transaction_id > " + this.geLastTxInIndex(),
                        Long.class);
        return result != null ? result : -1L;
    }

    public void clearCache() {
        this.getSolrAdminClient().clearCache();
    }

    public class Transaction {

        private long txId;
        private int count;

        public Transaction(long txId, int count) {
            this.txId = txId;
            this.count = count;
        }

        public long getTxId() {
            return txId;
        }

        public int getCount() {
            return count;
        }
    }

    public long getModelErrors() {
        try {
            long count = 0;
            JsonNode json = this.getSolrAdminClient().getSolrSummaryJson();

            if (json.has("Error") && json.get("Error").asInt() == -2) {
                return -1;
            }

            JsonNode alfrescoerror = null;
            JsonNode archiveerror = null;
            try {
                alfrescoerror = json.get("alfresco")
                        .get("Model changes are not compatible with the existing data model and have not been applied");
                //check archive null
                archiveerror = (json.get("archive") == null) ?
                        null : json.get("archive")
                        .get("Model changes are not compatible with the existing data model and have not been applied");
            } catch (NullPointerException e) {
                logger.debug("no model errors found.");
            }
            if (alfrescoerror == null && archiveerror == null) {
                return 0;
            }
            if (alfrescoerror != null) {
                count += geterrors(alfrescoerror);
            }
            if (archiveerror != null) {
                count += geterrors(archiveerror);
            }
            logger.debug("count: " + count);
            return count;
        } catch (Exception e) {
            logger.warn("Can't determine if there are Solr model errors");
        }
        return -1;
    }

    private long geterrors(JsonNode json) throws NullPointerException {
        long count = 0;
        logger.debug("Keys: " + json.fieldNames());
        Iterator<String> keys = json.fieldNames();
        while (keys.hasNext()) {
            logger.debug("Inside loop");
            String key = keys.next();
            try {
                count += json.get(key).size();
            } catch (Exception e) {
                count++;
            }
        }
        return count;
    }
}
