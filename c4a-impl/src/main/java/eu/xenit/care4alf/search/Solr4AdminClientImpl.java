package eu.xenit.care4alf.search;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
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
    public JSONObject getSolrErrorsJson(int start, int rows) throws JSONException, EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        parameters.put("q", "DOC_TYPE:ErrorNode");
        parameters.put("start", Integer.toString(start));
        parameters.put("rows", Integer.toString(rows));
        return getSolrClient().postJSON("/" + getSolrTypeUrl() + "/alfresco/select", parameters, null);
    }

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
                    doc.optString("id", ""),
                    doc.optLong("DBID", Long.parseLong(doc.optString("DBID", "-1"))),
                    doc.has("EXCEPTIONSTACK") ? doc.getJSONArray("EXCEPTIONSTACK").getString(0) : ""
            );
            errorDocs.add(errorDoc);
        }
        return errorDocs;
    }

    @Override
    protected String getSolrTypeUrl() {
        return "solr4";
    }

}