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
 * Created by willem on 5/16/17.
 */
@Component
public class Solr6AdminClientImpl extends Solr4AdminClientImpl{
    @Override
    protected String getSolrTypeUrl() {
        return "solr";
    }

}
