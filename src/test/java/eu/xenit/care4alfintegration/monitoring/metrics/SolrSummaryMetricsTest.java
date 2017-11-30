package eu.xenit.care4alfintegration.monitoring.metrics;

import eu.xenit.care4alf.monitoring.metric.SolrSummaryMetrics;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/**
 * Created by willem on 12/19/16.
 */
public class SolrSummaryMetricsTest {

    @Test
    public void testFlatten() throws JSONException, IOException {
        Map<String,String> map = SolrSummaryMetrics.flatten(new JSONObject("" +
                "{_:" +
                "   {1:" +
                "       {2:" +
                "           {3:value1}" +
                "       }" +
                "   }," +
                "  2:value2," +
                "  3:" +
                "     {2:value3}}"));
        Assert.assertEquals(3,map.keySet().size());
        Assert.assertEquals("value1",map.get("_.1.2.3"));
        Assert.assertEquals("value2",map.get("2"));
        Assert.assertEquals("value3",map.get("3.2"));
    }

    @Test
    public void testTransform() throws JSONException, IOException {
        Map<String,Long> map = SolrSummaryMetrics.flattenAndCleanup(new JSONObject("{a:5}"));
        Assert.assertEquals(1,map.keySet().size());
        Assert.assertEquals((Long)5L,map.get("solr.summary.a"));
    }

    @Test
    public void testIgnoreNonNumber() throws JSONException, IOException {
        Map<String,Long> map = SolrSummaryMetrics.flattenAndCleanup(new JSONObject("{a:ignore}"));
        Assert.assertEquals(0,map.keySet().size());
    }

    @Test
    public void testCleanupSpace() throws JSONException, IOException {
        Map<String,Long> map = SolrSummaryMetrics.flattenAndCleanup(new JSONObject("{alfresco.Alfresco Error Nodes in Index:0}"));
        Assert.assertEquals(1,map.keySet().size());
        Assert.assertEquals((Long)0L, map.get("solr.summary.alfresco.AlfrescoErrorNodesinIndex"));
    }

    @Test
    public void testCleanupSpecialchar() throws JSONException, IOException {
        Map<String,Long> map = SolrSummaryMetrics.flattenAndCleanup(new JSONObject("{\"alfresco./filterCache.warmupTime\":1}"));
        Assert.assertEquals(1,map.keySet().size());
        Assert.assertEquals((Long)1L,map.get("solr.summary.alfresco.filterCache.warmupTime"));
    }

    @Test
    public void testTransformSeconds(){
        // alfresco.TX Lag=0 s
        Assert.assertEquals((Long)0L,SolrSummaryMetrics.transformValue("0 s"));
        Assert.assertEquals((Long)0L,SolrSummaryMetrics.transformValue("0 Seconds"));
    }

    @Test
    public void testTransformBoolean(){
        //alfresco.ModelTracker Active=false
        Assert.assertEquals((Long)0L,SolrSummaryMetrics.transformValue("false"));
        Assert.assertEquals((Long)1L,SolrSummaryMetrics.transformValue("true"));
    }

    @Test
    public void testTransformNumbers(){
        //alfresco.ModelTracker Active=false
        Assert.assertEquals((Long)0L,SolrSummaryMetrics.transformValue("NaN"));
        Assert.assertEquals((Long)0L,SolrSummaryMetrics.transformValue("null"));
    }

    @Test
    public void testTransformFloat(){
        //alfresco.Acl index time (ms).Varience
        //alfresco.On disk (GB)=0.001589
        Assert.assertEquals((Long)1L,SolrSummaryMetrics.transformValue("0.001589"));
        Assert.assertEquals((Long)11L,SolrSummaryMetrics.transformValue("10.612132"));
        //Assert.assertEquals((Long)0L,SolrSummaryMetrics.transformValue("3.548307686694029E-4"));
    }

}
