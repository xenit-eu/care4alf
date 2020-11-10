package eu.xenit.care4alf.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.EncoderException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by willem on 9/26/16.
 */
@Component
public class Solr1AdminClientImpl extends AbstractSolrAdminClient {

    @Override
    public JsonNode getSolrErrorsJson(int start, int rows) throws EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        parameters.put("q", "ID:ERROR-*");
        parameters.put("start", Integer.toString(start));
        parameters.put("rows", Integer.toString(rows));
        return getSolrClient().postJSON("/" + getSolrTypeUrl() + "/alfresco/select", parameters, null);
    }

    @Override
    public List<SolrErrorDoc> getSolrErrorDocs(int rows) throws IOException, EncoderException {
        List<SolrErrorDoc> errorDocs = new ArrayList<SolrErrorDoc>();
        JsonNode json = this.getSolrErrorsJson(0, rows);
        ArrayNode docs = (ArrayNode) json.get("response").get("docs");
        for (JsonNode doc : docs) {
            SolrErrorDoc errorDoc = new SolrErrorDoc(
                    doc.hasNonNull("INTXID") ? doc.get("INTXID").get(0).asLong() : -1,
                    doc.hasNonNull("EXCEPTIONMESSAGE") ? doc.get("EXCEPTIONMESSAGE").get(0).asText() : "",
                    doc.hasNonNull("ID") ? doc.get("ID").get(0).asText() : "",
                    doc.hasNonNull("DBID") ? doc.get("DBID").get(0).asLong() : -1,
                    doc.hasNonNull("EXCEPTIONSTACK") ? doc.get("EXCEPTIONSTACK").get(0).asText() : ""
            );
            errorDocs.add(errorDoc);
        }
        return errorDocs;
    }

    @Override
    protected String getSolrTypeUrl() {
        return "solr";
    }

}