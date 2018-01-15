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

    private JSONObject solrSummaryActionJSON;

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

    public abstract JSONObject getSolrErrorsJson(int start, int rows) throws JSONException, EncoderException, IOException;

    protected abstract String getSolrTypeUrl();

    // This function uses a cached solrSummaryActionJSON. If new data is required call clearCache() first.
    JSONObject getSolrSummaryJson() throws JSONException, EncoderException, IOException {
        if(solrSummaryActionJSON == null) {
            Multimap<String, String> parameters = ArrayListMultimap.create();
            parameters.put("wt", "json");
            parameters.put("action", "SUMMARY");
            solrSummaryActionJSON = this.getSolrClient().postJSON("/" + getSolrTypeUrl() + "/admin/cores", parameters, null);
            return solrSummaryActionJSON.getJSONObject("Summary");
        } else {
            return solrSummaryActionJSON.getJSONObject("Summary");
        }
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

    void clearCache(){
        solrSummaryActionJSON = null;
    }
}