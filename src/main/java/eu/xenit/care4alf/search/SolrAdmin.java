package eu.xenit.care4alf.search;

import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.EncoderException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by willem on 3/1/16.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/solr", families = {"care4alf"}, description = "Solr administration")
public class SolrAdmin {
    @Autowired
    private SolrClient solrClient;

    @Uri("errors")
    public void errors(final WebScriptResponse response, @RequestParam(defaultValue = "0") String start, @RequestParam(defaultValue = "100") String rows) throws JSONException, EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        parameters.put("q", "ID:ERROR-*");
        parameters.put("start", start);
        parameters.put("rows", rows);
        JSONObject json = solrClient.post("/solr/alfresco/select", parameters);
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }

    public int getSolrErrors() throws JSONException, EncoderException, IOException {
        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("wt", "json");
        parameters.put("q", "ID:ERROR-*");
        parameters.put("start", "0");
        parameters.put("rows", "0");
        JSONObject json = solrClient.post("/solr/alfresco/select", parameters);
        return json.getJSONObject("response").getInt("numFound");
    }

    @Uri("proxy/{uri}")
    public void proxy(final WebScriptRequest request, final WebScriptResponse response, @UriVariable("uri") String uri) throws JSONException, EncoderException, IOException {
        String[] names = request.getParameterNames();
        Multimap<String, String> parameters = ArrayListMultimap.create();
        for(String name : names)
        {
            parameters.put(name, request.getParameter(name));
        }
        JSONObject json = solrClient.post("/solr/" + uri, parameters);
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }
}