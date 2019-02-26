package eu.xenit.care4alf.integrity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.joda.time.Interval;

public class IntegrityReport {
    private int scannedNodes;
    private ConcurrentHashMap<NodeRef, List<NodeProblem>> nodeProblems;
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
        NodeRef ref = problem.getNoderef();
        List<NodeProblem> problems;
        if (nodeProblems.containsKey(ref)) {
            problems = nodeProblems.get(ref);
        } else {
            problems = new ArrayList<>();
        }
        problems.add(problem);
        nodeProblems.put(ref, problems);
    }

    @JsonSerialize(keyUsing = NoderefFieldSerializer.class)
    public Map<NodeRef, List<NodeProblem>> getNodeProblems() {
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
