package eu.xenit.care4alf.monitoring.metric;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.dynamicextensionsalfresco.schedule.ScheduledTask;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import eu.xenit.care4alf.search.SolrAdmin;
import org.apache.commons.codec.EncoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by willem on 12/19/16.
 */
@Component
@ScheduledTask(name = "SolrSummaryMetrics", group = Monitoring.SCHEDULE_GROUP, cron = "0 0/5 * * * ?", cronProp = "c4a.monitoring.solrsummary.cron")
public class SolrSummaryMetrics extends AbstractMonitoredSource {
    private static Logger logger = LoggerFactory.getLogger(SolrSummaryMetrics.class);

    @Autowired
    private SolrAdmin solrAdmin;

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        try {
            this.solrAdmin.clearCache();
            JsonNode summary = this.solrAdmin.getSolrSummaryJson();
            Map<String,String> flattened = flatten(summary);
            Map<String, Long> r = transform(flattened);
            r.put("solr.errors", this.solrAdmin.getSolrErrors());
            r.put("solr.lag.time", this.solrAdmin.getSolrLag());
            r.put("solr.lag.nodes", this.solrAdmin.getNodesToIndex());
            r.put("solr.model.errors", this.solrAdmin.getModelErrors());
            return r;
        } catch (NullPointerException | EncoderException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Long> flattenAndCleanup(JsonNode jsonNode) {
        return transform(flatten(jsonNode));
    }

    private static Map<String,Long> transform(Map<String, String> map) {
        Map<String,Long> r = new HashMap<>();
        for(Map.Entry<String,String> entry: map.entrySet()){
            try {
                r.put(
                        "solr.summary."+transformKey(entry.getKey()),
                        transformValue(entry.getValue()));
            }
            catch (NumberFormatException e){
                logger.debug("Can't parse {}",entry.toString());
            }
        }
        return r;
    }

    public static String transformKey(String key){
        return key.replace(" ","")
                .replace("/","");
    }

    public static Long transformValue(String value){
        {
            if (value.matches("\\d*"))
                return Long.parseLong(value);
        }

        {
            Pattern secondsPattern = Pattern.compile("(\\d+) (s|Seconds)");
            Matcher matcher = secondsPattern.matcher(value);
            if (matcher.find())
                return Long.parseLong(matcher.group(1));
        }

        {
            if(value.equals("true")) return 1L;
            if(value.equals("false")) return 0L;
        }

        {
            if(value.equals("NaN")) return 0L;
            if(value.equals("null")) return 0L;
        }

        {
            if (value.matches("(\\d*\\.\\d*)"))
                return (long)(Math.ceil(Float.parseFloat(value)));
        }

        throw new NumberFormatException();
    }

    public static Map<String,String> flatten(JsonNode jsonNode) {
        Map<String, String> map = new HashMap<>();
        addKeys("", jsonNode, map);
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
