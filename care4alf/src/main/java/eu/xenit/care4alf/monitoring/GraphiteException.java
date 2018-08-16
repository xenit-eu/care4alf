package eu.xenit.care4alf.monitoring;

/**
 * Created by willem on 12/13/16.
 */
public class GraphiteException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    public GraphiteException(String message) {
        super(message);
    }

    public GraphiteException(String message, Throwable cause) {
        super(message, cause);
    }
}
