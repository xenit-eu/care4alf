package eu.xenit.care4alf.search;

import com.google.common.collect.Multimap;
import org.apache.commons.codec.EncoderException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Send POST's to Solr and return a JSON result
 *
 * @author Laurent Van der Linden
 */
public interface SolrClient {
    JSONObject post(String url, Multimap<String, String> parameters) throws IOException, EncoderException, JSONException;
    JSONObject post(String url, Multimap<String, String> parameters, JSONObject body) throws IOException, EncoderException, JSONException;
}
