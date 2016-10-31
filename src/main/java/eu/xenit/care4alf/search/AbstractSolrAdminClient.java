package eu.xenit.care4alf.search;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.EncoderException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

/**
 * Created by willem on 9/26/16.
 */
public abstract class AbstractSolrAdminClient {
    @Autowired
    private SolrClient solrClient;

    public List<SolrErrorDoc> getSolrErrorDocs() throws IOException, JSONException, EncoderException{
        return this.getSolrErrorDocs(100);
    }
    public abstract List<SolrErrorDoc> getSolrErrorDocs(int rows) throws IOException, JSONException, EncoderException;

    protected JSONObject getSolrErrorsJson(int start, int rows) throws JSONException, EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        //parameters.put("q", "ID:ERROR-*");
        parameters.put("q", "ERROR*");
        parameters.put("start", Integer.toString(start));
        parameters.put("rows", Integer.toString(rows));
        return solrClient.postJSON("/" + getSolrTypeUrl() + "/alfresco/" + selectOrQuery(), parameters, null);
    }

    protected abstract String selectOrQuery();
    protected abstract String getSolrTypeUrl();
}
