package eu.xenit.care4alf.unittest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import eu.xenit.care4alf.search.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Created by willem on 9/27/16.
 */
public class SolrAdminClientTest {
    @Mock
    private SolrFacade solrFacade;

    @Before
    public void setupMock() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testParseSolr1ErrorsJson() throws Exception {
        JsonNode jsonSolr1Errors = new ObjectMapper().readTree(solrErrors1);
        when(solrFacade.postJSON(anyString(),any(ArrayListMultimap.class),nullable(JsonNode.class))).thenReturn(jsonSolr1Errors);
        AbstractSolrAdminClient client = new Solr1AdminClientImpl();
        client.setSolrClient(solrFacade);
        List<SolrErrorDoc> errorDocs = client.getSolrErrorDocs();
        Assert.assertTrue(errorDocs.size() >= 2);

        SolrErrorDoc doc = errorDocs.get(0);
        Assert.assertEquals(8712874, doc.getTxid());
        Assert.assertEquals("Read timed out", doc.getException());
        Assert.assertEquals("ERROR-5928313", doc.getId());
        Assert.assertEquals(5928313, doc.getDbid());
    }

    @Test
    public void testParseSolr4ErrorsJson() throws Exception {
        JsonNode jsonSolr4Errors = new ObjectMapper().readTree(solrErrors4);
        when(solrFacade.postJSON(anyString(),any(ArrayListMultimap.class),nullable(JsonNode.class))).thenReturn(jsonSolr4Errors);
        AbstractSolrAdminClient client = new Solr4AdminClientImpl();
        client.setSolrClient(solrFacade);
        List<SolrErrorDoc> errorDocs = client.getSolrErrorDocs();
        Assert.assertTrue(errorDocs.size() >= 1);

        SolrErrorDoc doc = errorDocs.get(0);
        Assert.assertEquals(2337, doc.getDbid());
    }

    final static String solrErrors1="{\n" +
            "  \"response\": {\n" +
            "    \"start\": 0,\n" +
            "    \"docs\": [\n" +
            "      {\n" +
            "        \"INTXID\": [\n" +
            "          \"8712874\"\n" +
            "        ],\n" +
            "        \"EXCEPTIONMESSAGE\": [\n" +
            "          \"Read timed out\"\n" +
            "        ],\n" +
            "        \"ID\": [\n" +
            "          \"ERROR-5928313\"\n" +
            "        ],\n" +
            "        \"DBID\": [\n" +
            "          \"5928313\"\n" +
            "        ],\n" +
            "        \"EXCEPTIONSTACK\": [\n" +
            "          \"java.net.SocketTimeoutException: Read timed out at java.net.SocketInputStream.socketRead0(Native Method) at java.net.SocketInputStream.read(SocketInputStream.java:152) at java.net.SocketInputStream.read(SocketInputStream.java:122) at sun.security.ssl.InputRecord.readFully(InputRecord.java:442) at sun.security.ssl.InputRecord.read(InputRecord.java:480) at sun.security.ssl.SSLSocketImpl.readRecord(SSLSocketImpl.java:934) at sun.security.ssl.SSLSocketImpl.readDataRecord(SSLSocketImpl.java:891) at sun.security.ssl.AppInputStream.read(AppInputStream.java:102) at java.io.BufferedInputStream.fill(BufferedInputStream.java:235) at java.io.BufferedInputStream.read(BufferedInputStream.java:254) at org.apache.commons.httpclient.HttpParser.readRawLine(HttpParser.java:78) at org.apache.commons.httpclient.HttpParser.readLine(HttpParser.java:106) at org.apache.commons.httpclient.HttpConnection.readLine(HttpConnection.java:1116) at org.apache.commons.httpclient.MultiThreadedHttpConnectionManager$HttpConnectionAdapter.readLine(MultiThreadedHttpConnectionManager.java:1413) at org.apache.commons.httpclient.HttpMethodBase.readStatusLine(HttpMethodBase.java:1973) at org.apache.commons.httpclient.HttpMethodBase.readResponse(HttpMethodBase.java:1735) at org.apache.commons.httpclient.HttpMethodBase.execute(HttpMethodBase.java:1098) at org.apache.commons.httpclient.HttpMethodDirector.executeWithRetry(HttpMethodDirector.java:398) at org.apache.commons.httpclient.HttpMethodDirector.executeMethod(HttpMethodDirector.java:171) at org.apache.commons.httpclient.HttpClient.executeMethod(HttpClient.java:397) at org.apache.commons.httpclient.HttpClient.executeMethod(HttpClient.java:323) at org.alfresco.httpclient.AbstractHttpClient.executeMethod(AbstractHttpClient.java:135) at org.alfresco.httpclient.AbstractHttpClient.sendRemoteRequest(AbstractHttpClient.java:111) at org.alfresco.httpclient.HttpClientFactory$HttpsClient.sendRequest(HttpClientFactory.java:371) at org.alfresco.solr.client.SOLRAPIClient.getTextContent(SOLRAPIClient.java:992) at org.alfresco.solr.tracker.CoreTracker.addContentPropertyToDoc(CoreTracker.java:2974) at org.alfresco.solr.tracker.CoreTracker.indexNode(CoreTracker.java:2558) at org.alfresco.solr.tracker.MultiThreadedCoreTracker$NodeIndexWorkerRunnable.doWork(MultiThreadedCoreTracker.java:812) at org.alfresco.solr.tracker.MultiThreadedCoreTracker$AbstractWorkerRunnable.run(MultiThreadedCoreTracker.java:753) at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145) at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615) at java.lang.Thread.run(Thread.java:745) \"\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"INTXID\": [\n" +
            "          \"8715401\"\n" +
            "        ],\n" +
            "        \"ID\": [\n" +
            "          \"ERROR-5935285\"\n" +
            "        ],\n" +
            "        \"DBID\": [\n" +
            "          \"5935285\"\n" +
            "        ],\n" +
            "        \"EXCEPTIONSTACK\": [\n" +
            "          \"java.net.SocketTimeoutException: Read timed out at java.net.SocketInputStream.socketRead0(Native Method) at java.net.SocketInputStream.read(SocketInputStream.java:152) at java.net.SocketInputStream.read(SocketInputStream.java:122) at sun.security.ssl.InputRecord.readFully(InputRecord.java:442) at sun.security.ssl.InputRecord.read(InputRecord.java:480) at sun.security.ssl.SSLSocketImpl.readRecord(SSLSocketImpl.java:934) at sun.security.ssl.SSLSocketImpl.readDataRecord(SSLSocketImpl.java:891) at sun.security.ssl.AppInputStream.read(AppInputStream.java:102) at java.io.BufferedInputStream.fill(BufferedInputStream.java:235) at java.io.BufferedInputStream.read(BufferedInputStream.java:254) at org.apache.commons.httpclient.HttpParser.readRawLine(HttpParser.java:78) at org.apache.commons.httpclient.HttpParser.readLine(HttpParser.java:106) at org.apache.commons.httpclient.HttpConnection.readLine(HttpConnection.java:1116) at org.apache.commons.httpclient.MultiThreadedHttpConnectionManager$HttpConnectionAdapter.readLine(MultiThreadedHttpConnectionManager.java:1413) at org.apache.commons.httpclient.HttpMethodBase.readStatusLine(HttpMethodBase.java:1973) at org.apache.commons.httpclient.HttpMethodBase.readResponse(HttpMethodBase.java:1735) at org.apache.commons.httpclient.HttpMethodBase.execute(HttpMethodBase.java:1098) at org.apache.commons.httpclient.HttpMethodDirector.executeWithRetry(HttpMethodDirector.java:398) at org.apache.commons.httpclient.HttpMethodDirector.executeMethod(HttpMethodDirector.java:171) at org.apache.commons.httpclient.HttpClient.executeMethod(HttpClient.java:397) at org.apache.commons.httpclient.HttpClient.executeMethod(HttpClient.java:323) at org.alfresco.httpclient.AbstractHttpClient.executeMethod(AbstractHttpClient.java:135) at org.alfresco.httpclient.AbstractHttpClient.sendRemoteRequest(AbstractHttpClient.java:111) at org.alfresco.httpclient.HttpClientFactory$HttpsClient.sendRequest(HttpClientFactory.java:371) at org.alfresco.solr.client.SOLRAPIClient.getTextContent(SOLRAPIClient.java:992) at org.alfresco.solr.tracker.CoreTracker.addContentPropertyToDoc(CoreTracker.java:2974) at org.alfresco.solr.tracker.CoreTracker.indexNode(CoreTracker.java:2558) at org.alfresco.solr.tracker.MultiThreadedCoreTracker$NodeIndexWorkerRunnable.doWork(MultiThreadedCoreTracker.java:812) at org.alfresco.solr.tracker.MultiThreadedCoreTracker$AbstractWorkerRunnable.run(MultiThreadedCoreTracker.java:753) at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145) at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615) at java.lang.Thread.run(Thread.java:745) \"\n" +
            "        ]\n" +
            "      }\n" +
            "    ],\n" +
            "    \"numFound\": 318\n" +
            "  },\n" +
            "  \"responseHeader\": {\n" +
            "    \"status\": 0,\n" +
            "    \"QTime\": 0,\n" +
            "    \"params\": {\n" +
            "      \"start\": \"0\",\n" +
            "      \"q\": \"ID:ERROR-*\",\n" +
            "      \"wt\": \"json\",\n" +
            "      \"rows\": \"2\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

    final static String solrErrors4 = "{\n" +
            "\"response\": {\n" +
            "\"docs\": [\n" +
            "{\n" +
            "\"_version_\": 0,\n" +
            "\"DBID\": 2337,\n" +
            "\"id\": \"_DEFAULT_!800000000000000d!8000000000000921\"\n" +
            "}\n" +
            "],\n" +
            "\"numFound\": 389,\n" +
            "\"start\": 0\n" +
            "},\n" +
            "\"responseHeader\": {\n" +
            "\"QTime\": 0,\n" +
            "\"params\": {\n" +
            "\"q\": \"ERROR*\",\n" +
            "\"start\": \"0\",\n" +
            "\"rows\": \"1\",\n" +
            "\"wt\": \"json\"\n" +
            "},\n" +
            "\"status\": 0\n" +
            "}\n" +
            "}";
}
