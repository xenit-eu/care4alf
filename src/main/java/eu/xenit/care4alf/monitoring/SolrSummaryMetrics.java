package eu.xenit.care4alf.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import eu.xenit.care4alf.integration.MonitoredSource;
import eu.xenit.care4alf.search.SolrAdmin;
import org.apache.commons.codec.EncoderException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by willem on 12/19/16.
 */
@Component
public class SolrSummaryMetrics implements MonitoredSource{
    private static Logger logger = LoggerFactory.getLogger(SolrSummaryMetrics.class);

    @Autowired
    private SolrAdmin solrAdmin;

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        try {
            JSONObject summary = this.solrAdmin.getSolrSummaryJson();
            Map<String,String> flattened = this.flatten(summary);
            return transform(flattened);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (EncoderException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Long> flattenAndCleanup(JSONObject jsonObject) throws IOException {
        return transform(flatten(jsonObject));
    }

    private static Map<String,Long> transform(Map<String, String> map) {
        Map<String,Long> r = new HashMap<>();
        for(Map.Entry<String,String> entry: map.entrySet()){
            try {
                r.put("solr.summary."+entry.getKey()
                        .replace(" ","")
                        .replace("/",""),
                        Long.parseLong(entry.getValue()));
            }catch (NumberFormatException e){
                logger.debug("Can't parse {}",entry.toString());
            }
        }
        return r;
    }

    public static Map<String,String> flatten(JSONObject jsonObject) throws IOException {
        Map<String, String> map = new HashMap<>();
        addKeys("", new ObjectMapper().readTree(jsonObject.toString()), map);
        return map;
    }

    private static void addKeys(String currentPath, JsonNode jsonNode, Map<String, String> map) {
        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";

            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                addKeys(pathPrefix + entry.getKey(), entry.getValue(), map);
            }
        } else if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            for (int i = 0; i < arrayNode.size(); i++) {
                addKeys(currentPath + "[" + i + "]", arrayNode.get(i), map);
            }
        } else if (jsonNode.isValueNode()) {
            ValueNode valueNode = (ValueNode) jsonNode;
            map.put(currentPath, valueNode.asText());
        }
    }
}
