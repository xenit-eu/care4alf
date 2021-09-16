package eu.xenit.care4alf.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.EncoderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Created by willem on 9/26/16.
 */
@Component
public abstract class AbstractSolrAdminClient {
    private SolrFacade solrFacade;

    private JsonNode solrSummaryActionJSON;

    protected SolrFacade getSolrClient() {
        return this.solrFacade;
    }

    @Autowired
    public void setSolrClient(SolrFacade solrFacade) {
        this.solrFacade = solrFacade;
    }

    public List<SolrErrorDoc> getSolrErrorDocs() throws IOException, EncoderException{
        return this.getSolrErrorDocs(100);
    }

    public abstract List<SolrErrorDoc> getSolrErrorDocs(int rows) throws IOException, EncoderException;

    public abstract JsonNode getSolrErrorsJson(int start, int rows) throws EncoderException, IOException;

    protected abstract String getSolrTypeUrl();

    // This function uses a cached solrSummaryActionJSON. If new data is required call clearCache() first.
    JsonNode getSolrSummaryJson() throws EncoderException, IOException {
        if(solrSummaryActionJSON == null) {
            Multimap<String, String> parameters = ArrayListMultimap.create();
            parameters.put("wt", "json");
            parameters.put("action", "SUMMARY");
            solrSummaryActionJSON = this.getSolrClient().postJSON("/" + getSolrTypeUrl() + "/admin/cores", parameters, null);
            return solrSummaryActionJSON.get("Summary");
        } else {
            return solrSummaryActionJSON.get("Summary");
        }
    }

    public JsonNode reindex(long dbId) throws EncoderException, IOException {
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