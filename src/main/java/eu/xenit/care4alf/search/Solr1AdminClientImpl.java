package eu.xenit.care4alf.search;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by willem on 9/26/16.
 */
public class Solr1AdminClientImpl implements SolrAdminClient {

    @Override
    public List<SolrErrorDoc> parseSolrErrorDocs(JSONObject json) throws JSONException {
        List<SolrErrorDoc> errorDocs = new ArrayList<SolrErrorDoc>();
        JSONArray docs = json.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            SolrErrorDoc errorDoc = new SolrErrorDoc(
                    doc.has("INTXID") ? Long.parseLong(doc.getJSONArray("INTXID").getString(0)) : -1,
                    doc.has("EXCEPTIONMESSAGE") ? doc.getJSONArray("EXCEPTIONMESSAGE").getString(0) : "",
                    doc.has("ID") ? doc.getJSONArray("ID").getString(0) : "",
                    doc.has("DBID") ? Long.parseLong(doc.getJSONArray("DBID").getString(0)) : -1,
                    doc.has("EXCEPTIONSTACK") ? doc.getJSONArray("EXCEPTIONSTACK").getString(0) : ""
            );
            errorDocs.add(errorDoc);
        }
        return errorDocs;
    }
}
