package eu.xenit.care4alf.search;

/**
 * Created by willem on 9/26/16.
 */
public class SolrErrorDoc {
    private long txid;
    private String exception;
    private String id;
    private long dbid;
    private String stackTrace;

    public SolrErrorDoc(long txid, String exception, String id, long dbid, String stackTrace) {
        this.txid = txid;
        this.exception = exception;
        this.id = id;
        this.dbid = dbid;
        this.stackTrace = stackTrace;
    }

    public long getTxid() {
        return txid;
    }

    public String getException() {
        return exception == null ? "" : exception;
    }

    public String getId() {
        return id;
    }

    public long getDbid() {
        return dbid;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
