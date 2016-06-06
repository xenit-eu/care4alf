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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
        return solrClient.post("/solr/alfresco/select", parameters);
    }

    public JSONObject getSolrSummary() throws JSONException, EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        parameters.put("action", "SUMMARY");
        return solrClient.post("/solr/admin/cores", parameters).getJSONObject("Summary");
    }

    public int getSolrErrors() throws JSONException, EncoderException, IOException {
        JSONObject json = this.getSolrErrorsJson(0,0);
        return json.getJSONObject("response").getInt("numFound");
    }

    @Uri("proxy/{uri}")
    public void proxy(final WebScriptRequest request, final WebScriptResponse response, @UriVariable("uri") String uri) throws JSONException, EncoderException, IOException {
        String[] names = request.getParameterNames();
        Multimap<String, String> parameters = ArrayListMultimap.create();
        for(String name : names)
        {
            parameters.put(name, request.getParameter(name));
        }
        JSONObject json = solrClient.post("/solr/" + uri, parameters);
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
                    nodeRef.toString(),
                    this.nodeService.getType(nodeRef).toString(),
                    (String) this.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME),
                    this.nodeService.getProperty(nodeRef, ContentModel.PROP_CREATED).toString(),
                    this.nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED).toString(),
                    Long.toString(size)
            };
            for(String field : fields){
                sb.append(field).append(",");
            }
            sb.append("\n");
        }
        return sb.toString();
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

    public long getSolrLag() throws EncoderException, JSONException, IOException {
        JSONObject summary = this.getSolrSummary();
        String lag = summary.getJSONObject("alfresco").getString("TX Lag");
        return Long.parseLong(lag.replace(" s",""));
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
            return exception;
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

}