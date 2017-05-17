package eu.xenit.care4alf.search;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.EncoderException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Created by willem on 9/26/16.
 */
@Component
public abstract class AbstractSolrAdminClient {
    private SolrClient solrClient;

    protected SolrClient getSolrClient() {
        return this.solrClient;
    }

    @Autowired
    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public List<SolrErrorDoc> getSolrErrorDocs() throws IOException, JSONException, EncoderException{
        return this.getSolrErrorDocs(100);
    }

    public abstract List<SolrErrorDoc> getSolrErrorDocs(int rows) throws IOException, JSONException, EncoderException;

    public JSONObject getSolrErrorsJson(int start, int rows) throws JSONException, EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        //parameters.put("q", "ID:ERROR-*");
        parameters.put("q", "ERROR*");
        parameters.put("start", Integer.toString(start));
        parameters.put("rows", Integer.toString(rows));
        return getSolrClient().postJSON("/" + getSolrTypeUrl() + "/alfresco/" + selectOrQuery(), parameters, null);
    }

    protected abstract String selectOrQuery();
    protected abstract String getSolrTypeUrl();

    public JSONObject getSolrSummaryJson() throws JSONException, EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        parameters.put("action", "SUMMARY");
        return this.getSolrClient().postJSON("/" + getSolrTypeUrl() + "/admin/cores", parameters, null).getJSONObject("Summary");
    }

    public JSONObject reindex(long dbId) throws JSONException, EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        parameters.put("action", "REINDEX");
        parameters.put("nodeid", String.valueOf(dbId));
        return getSolrClient().postJSON("/" + getSolrTypeUrl() + "/admin/cores", parameters, null);
    }

    public String optimize() throws IOException, EncoderException {
        return this.getSolrClient().postMessage("/" + getSolrTypeUrl() + "/alfresco/update", null, "<optimize />");
    }

}