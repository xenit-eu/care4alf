package eu.xenit.care4alf.search;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.codec.EncoderException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
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
import java.util.ArrayList;
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
    private SolrClient solrClient;

    @Autowired
    private DataSource dataSource;

    @Uri("errors")
    public void errors(final WebScriptResponse response, @RequestParam(defaultValue = "0") String start, @RequestParam(defaultValue = "100") String rows) throws JSONException, EncoderException, IOException {
        JSONObject json = this.getSolrErrorsJson(Integer.parseInt(start), Integer.parseInt(rows));
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }

    public JSONObject getSolrErrorsJson(int start, int rows) throws JSONException, EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        parameters.put("q", "ID:ERROR-*");
        parameters.put("start", Integer.toString(start));
        parameters.put("rows", Integer.toString(rows));
        return solrClient.postJSON("/solr/alfresco/select", parameters, null);
    }

    public JSONObject getSolrSummary() throws JSONException, EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        parameters.put("action", "SUMMARY");
        return solrClient.postJSON("/solr/admin/cores", parameters, null).getJSONObject("Summary");
    }

    public long getSolrErrors() {
        try {
            JSONObject json = this.getSolrErrorsJson(0,0);
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
        for(String name : names)
        {
            parameters.put(name, request.getParameter(name));
        }
        JSONObject json = solrClient.postJSON("/solr/" + uri, parameters, null);
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }

    @Uri("errors/nodes")
    public void getRESTSolrErrorNodes(final WebScriptResponse response, @RequestParam(defaultValue = "100") int rows) throws IOException, JSONException, EncoderException {
        response.getWriter().write(this.getSolrErrorNodes(rows));
    }

    @Uri(value = "errors/nodes/fix/{filter}", method = HttpMethod.POST)
    public void fixSolrErrors(
            final WebScriptResponse res,
            @UriVariable final String filter,
            @RequestParam(defaultValue = "100") final int rows) throws IOException, JSONException, EncoderException {
        List<SolrErrorDoc> docs = this.getSolrErrorDocs(rows);
        int count = 0;
        logger.debug("filter: '{}'",filter);
        for(SolrErrorDoc doc : docs) {
            logger.debug("Exception: '{}'",doc.getException());
            if(doc.getException().equals(filter)){
                NodeRef nodeRef = nodeService.getNodeRef(doc.getDbid());
                nodeService.setProperty(nodeRef,ContentModel.PROP_IS_CONTENT_INDEXED,false);
                count++;
            }
        }
        res.getWriter().write(Integer.toString(count));
    }

    public String getSolrErrorNodes(int rows) throws EncoderException, JSONException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("exception;txid;dbid;noderef;type;name;created;modified;filesize\n");
        List<SolrErrorDoc> docs = this.getSolrErrorDocs(rows);
        for(SolrErrorDoc doc : docs){
            NodeRef nodeRef = this.nodeService.getNodeRef(doc.getDbid());
            ContentData content = (ContentData) this.nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
            long size = -1;
            if(content != null)
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
            for(String field : fields){
                sb.append(field).append(",");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String orEmpty(Object o){
        return o == null?  "" : o.toString();
    }

    public List<SolrErrorDoc> getSolrErrorDocs(int rows) throws EncoderException, JSONException, IOException {
        JSONObject json = this.getSolrErrorsJson(0,rows);
        return parseSolrErrorDocs(json);
    }

    public List<SolrErrorDoc> parseSolrErrorDocs(JSONObject json) throws JSONException {
        List<SolrErrorDoc> errorDocs = new ArrayList<SolrErrorDoc>();
        JSONArray docs = json.getJSONObject("response").getJSONArray("docs");
        for(int i = 0; i < docs.length(); i++){
            JSONObject doc = docs.getJSONObject(i);
            SolrErrorDoc errorDoc = new SolrErrorDoc(
                    doc.has("INTXID")?Long.parseLong(doc.getJSONArray("INTXID").getString(0)):-1,
                    doc.has("EXCEPTIONMESSAGE")?doc.getJSONArray("EXCEPTIONMESSAGE").getString(0):"",
                    doc.has("ID")?doc.getJSONArray("ID").getString(0):"",
                    doc.has("DBID")?Long.parseLong(doc.getJSONArray("DBID").getString(0)):-1,
                    doc.has("EXCEPTIONSTACK")?doc.getJSONArray("EXCEPTIONSTACK").getString(0):""
                    );
            errorDocs.add(errorDoc);
        }
        return errorDocs;
    }

    public long getSolrLag(){
        JSONObject summary = null;
        try {
            summary = this.getSolrSummary();
            String lag = summary.getJSONObject("alfresco").getString("TX Lag");
            return Long.parseLong(lag.replace(" s",""));
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
        res.getWriter().write(solrClient.postMessage("/solr/alfresco/update", null, "<optimize />"));
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
                        return new Transaction(rs.getLong(1),rs.getInt(2));
                    }
                });
    }

    private long geLastTxInIndex(){
        JSONObject summary = null;
        try {
            summary = this.getSolrSummary();
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

    public class SolrErrorDoc{
        private long txid;
        private String exception;
        private String id;
        private long dbid;
        private String stackTrace;

        public SolrErrorDoc(long txid, String exception, String id, long dbid, String stackTrace) {
            this.txid = txid;
            this.exception = exception;
            this.id = id;
            this.dbid = dbid;
            this.stackTrace = stackTrace;
        }

        public long getTxid() {
            return txid;
        }

        public String getException() {
            return orEmpty(exception);
        }

        public String getId() {
            return id;
        }

        public long getDbid() {
            return dbid;
        }

        public String getStackTrace() {
            return stackTrace;
        }
    }

    public class Transaction{
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

}