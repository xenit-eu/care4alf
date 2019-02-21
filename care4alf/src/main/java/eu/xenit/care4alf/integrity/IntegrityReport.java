package eu.xenit.care4alf.integrity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.joda.time.Interval;

public class IntegrityReport {
    private int scannedNodes;
    private ConcurrentHashMap<NodeRef, NodeProblem> nodeProblems;
    private Date startTime;
    private Date endTime;

    public IntegrityReport() {
        startTime = new Date();
        nodeProblems = new ConcurrentHashMap<>();
    }

    public void finish() {
        endTime = new Date();
    }

    public void addNodeProblem(NodeProblem problem) {
        nodeProblems.put(problem.getNoderef(), problem);
    }

    @JsonSerialize(keyUsing = NoderefFieldSerializer.class)
    public Map<NodeRef, NodeProblem> getNodeProblems() {
        return nodeProblems;
    }

    public Map<String, Problem> getFileProblems() {
        return Collections.emptyMap();
    }

    public int getScannedNodes() {
        return scannedNodes;
    }

    public void setScannedNodes(int scannedNodes) {
        this.scannedNodes = scannedNodes;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    @JsonGetter("runtime (ms)")
    public long getRuntime() {
        return new Interval(getStartTime().getTime(), getEndTime().getTime()).toDurationMillis();
    }
}
