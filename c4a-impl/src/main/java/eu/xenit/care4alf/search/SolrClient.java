package eu.xenit.care4alf.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.EncoderException;

import java.io.IOException;

/**
 * Send POST's to Solr and return a JSON result
 *
 * @author Laurent Van der Linden
 */
public interface SolrClient {
    JsonNode postJSON(String url, Multimap<String, String> parameters, JsonNode body) throws IOException, EncoderException;
    String postMessage(String url, Multimap<String, String> parameter, String message) throws IOException, EncoderException;

    String get(String url, Multimap<String, String> parameters) throws IOException, EncoderException;
}
