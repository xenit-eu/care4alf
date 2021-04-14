package eu.xenit.care4alf.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import eu.xenit.care4alf.search.subsystemconditions.Solr4Condition;
import org.apache.commons.codec.EncoderException;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by willem on 9/26/16.
 */
@Component
@Conditional(Solr4Condition.class)
public class Solr4AdminClientImpl extends AbstractSolrAdminClient{

    @Override
    public JsonNode getSolrErrorsJson(int start, int rows) throws EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        parameters.put("q", "DOC_TYPE:ErrorNode");
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
                    -1,
                    "",
                    doc.hasNonNull("id") ? doc.get("id").asText() : "",
                    doc.hasNonNull("DBID") ? doc.get("DBID").asLong() : -1L,
                    doc.hasNonNull("EXCEPTIONSTACK") ? doc.get("EXCEPTIONSTACK").get(0).asText() : ""
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