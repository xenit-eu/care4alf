package eu.xenit.care4alf.export;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import com.google.common.collect.ImmutableMultimap;
import eu.xenit.care4alf.search.SolrClient;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.codec.EncoderException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.sql.*;
import java.util.*;

/**
 * Created by willem on 6/6/15.
 */
@Component
@Authentication(AuthenticationType.USER)
@Transaction(TransactionType.REQUIRED)
@WebScript(baseUri = "/xenit/care4alf/nlp")
public class Nlp {
    private final static Logger logger = LoggerFactory.getLogger(Nlp.class);

    @Autowired
    NodeService nodeService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    ContentService contentService;

    @Autowired
    private SolrClient solrClient;

    @Autowired
    private SearchService searchService;

    @Autowired
    private DataSource dataSource;

    private Map<String, Integer> dfCache;

    public Nlp()
    {
        dfCache = new HashMap<String, Integer>();
    }

    public static HashMap<String, Integer> getTf(String content){
        String[] tokenizedTerms = content.toLowerCase().replaceAll("[\\W&&[^\\s]]", "").split("\\W+");
        HashMap<String, Integer> terms = new HashMap<String, Integer>(tokenizedTerms.length/2);
        for(String term : tokenizedTerms)
        {
            if(term.trim().equals(""))
                continue;
            if(!terms.containsKey(term))
                terms.put(term,1);
            else terms.put(term,terms.get(term)+1);
        }
        return terms;
    }

    public HashMap<String, Integer> getTermsOld(NodeRef nodeRef)
    {
        return getTf(this.getText(nodeRef));
    }

    public HashMap<String, Integer> getTerms(NodeRef nodeRef) throws IOException {

        HashMap<String, Integer> terms = new HashMap<String, Integer>();

        Analyzer analyzer = new SimpleAnalyzer();
        TokenStream stream = analyzer.tokenStream("content", new StringReader(getText(nodeRef)));
        Token t = stream.next();
        while(t != null)
        {
            String term = t.term();
            if(!terms.containsKey(term))
                terms.put(term,1);
            else terms.put(term,terms.get(term)+1);
            t = stream.next(t);
        }
        return terms;
    }

    public String getText(NodeRef nodeRef)
    {
        ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);

        TransformationOptions options = new TransformationOptions();
        options.setSourceNodeRef(nodeRef);
        List<ContentTransformer> transformers = contentService.getActiveTransformers(reader.getMimetype(), reader.getSize(), MimetypeMap.MIMETYPE_TEXT_PLAIN, options);

        if(transformers.size() == 0){
            //When there are no transformers, there is no text.
            return "";
        }

        ContentTransformer transformer = transformers.get(0);
        ContentWriter writer = contentService.getTempWriter();
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");


        transformer.transform(reader, writer);

        ContentReader textReader = writer.getReader();
        return (textReader != null && textReader.exists()) ? textReader.getContentString() : "";
    }

    public static double getTfIdf(int tf, int N, int df){
        if(df == 0)
            return 0;
//        return tf * Math.log(N/new Double(df));
        return tf / new Double(df);
    }

    public TreeMap<String, Double> getTfIdfs(NodeRef nodeRef) throws JSONException, EncoderException, IOException, SQLException {
        HashMap<String, Integer> tfs =  this.getTerms(nodeRef);
        HashMap<String, Double> tfidfs = new HashMap<String, Double>();
        int N = (int) this.getNumDocs();

        for(Map.Entry<String,Integer> tf : tfs.entrySet())
        {
            int df = getDocumentFrequency(tf.getKey());
            if(df <= 0)
                continue;
            tfidfs.put(tf.getKey(), getTfIdf(tf.getValue(), N, df));
        }

        return (TreeMap<String, Double>) sortByValue(tfidfs);
    }

    public long getNumDocs() throws SQLException {
        //TODO uitkomst niet correct, maar wel in de buurt
        long count = -1;
        final Connection connection = dataSource.getConnection();
        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM alf_node WHERE" +
                    " store_id = (select id from alf_store where protocol = 'workspace' and identifier = 'SpacesStore') AND" +
                    " type_qname_id IN (select id from alf_qname where local_name='content')");// 76 = 'content' en 5 = workspace/SpacesStore
            if (rs.next()) {
                count = rs.getLong(1);
            }
            rs.close();
        } finally {
            connection.close();
        }
        return count;
    }

    public int getDocumentFrequency(String term) throws EncoderException, IOException, JSONException {
        if(this.dfCache.containsKey(term))
            return this.dfCache.get(term);

        JSONObject json = solrClient.postJSON("/solr/alfresco/terms", ImmutableMultimap.<String, String>builder()
                .put("terms.fl", "@{http://www.alfresco.org/model/content/1.0}content.__")
                .put("terms.prefix", term)
                .put("terms.limit", "10")
                .put("wt", "json")
                .build(), null);
        JSONArray terms = json.getJSONArray("terms")
                .getJSONArray(1);
        for(int i = 0; i < terms.length(); i+=2){
            if(terms.get(i).equals(term)) {
                int df = ((Integer) terms.get(i + 1)) ; // -1 om het document zelf eruit te halen
                dfCache.put(term,df);
                return df;
            }
        }
        return 0;
    }

    @Uri("content/{noderef}")
    public void content(@UriVariable NodeRef noderef, final WebScriptResponse response) throws IOException {
        response.getWriter().write(this.getText(noderef));
    }

    @Uri("terms/{noderef}")
    public void terms(@UriVariable NodeRef noderef, final WebScriptResponse response) throws IOException {
        JSONObject json = new JSONObject(getTerms(noderef));
        response.getWriter().write(json.toString());
    }

    @Uri("df/{term}")
    public void term(@UriVariable String term, final WebScriptResponse response)
            throws JSONException, EncoderException, IOException {
        response.getWriter().write(Integer.toString(this.getDocumentFrequency(term)));
    }

    @Uri("tfidf/{noderef}")
    public void tfidf(@UriVariable NodeRef noderef, final WebScriptResponse response)
            throws JSONException, EncoderException, IOException, SQLException {
        Map<String, Double> tfidfs = this.getTfIdfs(noderef);
        final JSONWriter json = new JSONWriter(response.getWriter());
        int position = 1;
        json.object();
        json.key("path");
        json.value(this.nodeService.getPath(noderef).toDisplayPath(
                this.nodeService, permissionService));
        json.key("name");
        json.value(this.nodeService.getProperty(noderef, ContentModel.PROP_NAME));
        json.key("tfidfs");
        json.array();
        for(Map.Entry<String,Double> tfidf : tfidfs.entrySet())
        {
            json.object();
            json.key("token");json.value(tfidf.getKey());
            json.key("tfidf");json.value(tfidf.getValue());
            json.key("position");json.value(position++);
            json.endObject();
        }
        json.endArray();
        json.endObject();
    }

    public static Map sortByValue(Map unsortedMap) {
        Map sortedMap = new TreeMap(new ValueComparator(unsortedMap));
        sortedMap.putAll(unsortedMap);
        return sortedMap;
    }

    public static class ValueComparator implements Comparator {
        Map map;

        public ValueComparator(Map map) {
            this.map = map;
        }

        public int compare(Object keyA, Object keyB) {
            Comparable valueA = (Comparable) map.get(keyA);
            Comparable valueB = (Comparable) map.get(keyB);
            return valueA.compareTo(valueB)*-1;
        }
    }

}