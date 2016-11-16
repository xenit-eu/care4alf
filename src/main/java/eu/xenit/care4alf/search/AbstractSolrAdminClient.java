package eu.xenit.care4alf.search;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.EncoderException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Created by willem on 9/26/16.
 */
@Component
public abstract class AbstractSolrAdminClient {
    public List<SolrErrorDoc> getSolrErrorDocs() throws IOException, JSONException, EncoderException{
        return this.getSolrErrorDocs(100);
    }

    public abstract List<SolrErrorDoc> getSolrErrorDocs(int rows) throws IOException, JSONException, EncoderException;

    protected abstract String selectOrQuery();
    protected abstract String getSolrTypeUrl();
}
