package eu.xenit.care4alf.search;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import eu.xenit.care4alf.Config;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.codec.EncoderException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by willem on 3/1/16.
 */
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
    private Config config;

    @Uri("errors")
    public void errors(final WebScriptResponse response, @RequestParam(defaultValue = "0") String start, @RequestParam(defaultValue = "100") String rows) throws JSONException, EncoderException, IOException {
        JSONObject json = this.getSolrAdminClient().getSolrErrorsJson(Integer.parseInt(start), Integer.parseInt(rows));
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }

    private String getSearchSubSystemName(){
        return config.getProperty("index.subsystem.name");
    }

    public JSONObject getSolrSummaryJson() throws JSONException, EncoderException, IOException {
        return this.getSolrAdminClient().getSolrSummaryJson();
    }

    public long getSolrErrors() {
        try {
            JSONObject json = this.getSolrAdminClient().getSolrErrorsJson(0, 0);
            return json.getJSONObject("response").getLong("numFound");
        } catch (JSONException e) {
            return -1;
        } catch (EncoderException e) {
            return -1;
        } catch (IOException e) {
            return -1;
        }
    }

    @Uri("proxy/{uri}")
    public void proxy(final WebScriptRequest request, final WebScriptResponse response, @UriVariable("uri") String uri) throws JSONException, EncoderException, IOException {
        String[] names = request.getParameterNames();
        Multimap<String, String> parameters = ArrayListMultimap.create();
        for (String name : names) {
            parameters.put(name, request.getParameter(name));
        }
        String result = this.getSolrAdminClient().getSolrClient().get("/" + this.getSolrAdminClient().getSolrTypeUrl() + "/" + uri, parameters);
        response.setContentType("application/json");
        response.getWriter().write(result);
    }

    @Uri("errors/nodes")
    public void getRESTSolrErrorNodes(final WebScriptResponse response, @RequestParam(defaultValue = "100") int rows) throws IOException, JSONException, EncoderException {
        response.getWriter().write(this.getSolrErrorNodes(rows));
    }

    @Uri(value = "errors/nodes/fix/{filter}", method = HttpMethod.POST)
    public void fixSolrErrors(
            final WebScriptResponse res,
            @UriVariable() String filter,
            @RequestParam(defaultValue = "10000") final int rows,
            @RequestParam(defaultValue = "nocontent") final String action) throws IOException, JSONException, EncoderException {
        List<SolrErrorDoc> docs = this.getSolrAdminClient().getSolrErrorDocs(rows);
        int count = 0;
        if(filter == null) filter="all";
        logger.debug("filter: '{}'", filter);
        for (SolrErrorDoc doc : docs) {
            logger.debug("Processing document with id={}, Exception: '{}', Action={}", doc.getDbid(), doc.getException(), action);
            if ((filter.equals("all") || doc.getException().equals(filter))) {
                if(action.equals("nocontentindexing")){
                    logger.debug("Setting content-indexing to false");
                    NodeRef nodeRef = nodeService.getNodeRef(doc.getDbid());
                    nodeService.setProperty(nodeRef, ContentModel.PROP_IS_CONTENT_INDEXED, false);
                    count++;
                }
                else if(action.equals("reindex")){
                    logger.debug("Reindexing node");
                    this.getSolrAdminClient().reindex(doc.getDbid());
                    count++;
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    logger.debug("Ignoring document");
                }
            }
        }
        res.getWriter().write(Integer.toString(count));
    }

    public String getSolrErrorNodes(int rows) throws EncoderException, JSONException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("exception;txid;dbid;noderef;type;name;created;modified;filesize\n");
        List<SolrErrorDoc> docs = this.getSolrAdminClient().getSolrErrorDocs(rows);
        for (SolrErrorDoc doc : docs) {
            NodeRef nodeRef = this.nodeService.getNodeRef(doc.getDbid());
            ContentData content = (ContentData) this.nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
            long size = -1;
            if (content != null)
                size = content.getSize();
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

    @Autowired
    Solr1AdminClientImpl solr1AdminClient;

    @Autowired
    Solr4AdminClientImpl solr4AdminClient;

    @Autowired
    Solr6AdminClientImpl solr6AdminClient;

    public AbstractSolrAdminClient getSolrAdminClient() {
        String searchSubSystem = getSearchSubSystemName().toLowerCase();
        if(searchSubSystem.equals("solr4"))
            return solr4AdminClient;
        if(searchSubSystem.equals("solr6"))
            return solr6AdminClient;
        return solr1AdminClient;
    }

    public long getSolrLag() {
        JSONObject summary = null;
        try {
            summary = this.getSolrAdminClient().getSolrSummaryJson();
            String lag = summary.getJSONObject("alfresco").getString("TX Lag");
            return Long.parseLong(lag.replace(" s", ""));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (EncoderException e) {
            e.printStackTrace();
        } catch (IOException e) {
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
    public void getTransactionsToIndex(WebScriptResponse response, @RequestParam(required = false) Long txId) throws IOException {
        new ObjectMapper().writeValue(response.getWriter(), this.getTransactionsToIndex(txId == null ? this.geLastTxInIndex() : txId));
    }

    public List<Transaction> getTransactionsToIndex(long fromTxId) {
        return new JdbcTemplate(dataSource).query(
                "select TRANSACTION_ID, count(*) as n from alf_node where transaction_id >= " + Long.toString(fromTxId) + " group by TRANSACTION_ID order by n desc",
                new Object[]{},
                new RowMapper<Transaction>() {
                    @Override
                    public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new Transaction(rs.getLong(1), rs.getInt(2));
                    }
                });
    }

    private long geLastTxInIndex() {
        JSONObject summary = null;
        try {
            summary = this.getSolrAdminClient().getSolrSummaryJson();
            return summary.getJSONObject("alfresco").getLong("Id for last TX in index");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (EncoderException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Long.MAX_VALUE;
    }

    public long getNodesToIndex() {
        return new JdbcTemplate(dataSource).queryForLong("select count(*) as n from alf_node where transaction_id > " + this.geLastTxInIndex());
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

    public long getModelErrors(){
        try {
            long count = 0;
            JSONObject json = this.getSolrAdminClient().getSolrSummaryJson();

            if (json.has("Error") && (int) json.get("Error") == -2) {
                return -1;
            }

            Object alfrescoerror = null;
            Object archiveerror = null;
            try {
                alfrescoerror = json.getJSONObject("alfresco").get("Model changes are not compatible with the existing data model and have not been applied");
                //check archive null
                archiveerror = (json.getJSONObject("archive") == null) ?
                        null : json.getJSONObject("archive").get("Model changes are not compatible with the existing data model and have not been applied");
            } catch (JSONException e) {
                logger.debug("no model errors found.");
            }
            if (alfrescoerror == null && archiveerror == null) return 0;
            if (alfrescoerror != null) {
                count += geterrors(alfrescoerror);
            }
            if (archiveerror != null) {
                count += geterrors(archiveerror);
            }
            logger.debug("count: " + count);
            return count;
        }
        catch (Exception e){
            logger.warn("Can't determine if there are Solr model errors");
        }
        return -1;
    }

    private long geterrors(Object json) throws JSONException {
        long count = 0;
        JSONObject archerrorjson = ((JSONObject) json);
        logger.debug("Keys: " + archerrorjson.keys());
        Iterator<String> keys = archerrorjson.keys();
        while (keys.hasNext()) {
            logger.debug("Inside loop");
            String key = keys.next();
            try {
                count += archerrorjson.getJSONArray(key).length();
            } catch (Exception e) {
                count++;
            }
        }
        return count;
    }
}