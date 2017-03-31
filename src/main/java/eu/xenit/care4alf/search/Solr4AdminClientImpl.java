package eu.xenit.care4alf.search;

import org.apache.commons.codec.EncoderException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by willem on 9/26/16.
 */
@Component
public class Solr4AdminClientImpl extends AbstractSolrAdminClient{

    @Override
    public List<SolrErrorDoc> getSolrErrorDocs(int rows) throws IOException, JSONException, EncoderException {
        List<SolrErrorDoc> errorDocs = new ArrayList<SolrErrorDoc>();
        JSONObject json = this.getSolrErrorsJson(0, rows);
        JSONArray docs = json.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            SolrErrorDoc errorDoc = new SolrErrorDoc(
                    -1,
                    "",
                    doc.has("id") ? doc.getString("id") : "",
                    doc.has("DBID") ? Long.parseLong(doc.getString("DBID")) : -1,
                    doc.has("EXCEPTIONSTACK") ? doc.getJSONArray("EXCEPTIONSTACK").getString(0) : ""
            );
            errorDocs.add(errorDoc);
        }
        return errorDocs;
    }


    @Override
    protected String selectOrQuery() {
        return "select";
    }

    @Override
    protected String getSolrTypeUrl() {
        return "solr4";
    }

}