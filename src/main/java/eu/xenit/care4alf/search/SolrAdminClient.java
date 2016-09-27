package eu.xenit.care4alf.search;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by willem on 9/26/16.
 */
public interface SolrAdminClient {
    List<SolrErrorDoc> parseSolrErrorDocs(JSONObject json) throws JSONException;
}
