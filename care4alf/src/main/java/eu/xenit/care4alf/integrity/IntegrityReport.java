package eu.xenit.care4alf.integrity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.joda.time.Interval;

public class IntegrityReport {
    private int scannedNodes;
    private ConcurrentHashMap<NodeRef, List<NodeProblem>> nodeProblems;
    private ConcurrentHashMap<String, List<FileProblem>> fileProblems;
    private Date startTime;
    private Date endTime;

    public IntegrityReport() {
        startTime = new Date();
        nodeProblems = new ConcurrentHashMap<>();
        fileProblems = new ConcurrentHashMap<>();
    }

    public void finish() {
        endTime = new Date();
    }

    public void addNodeProblem(NodeProblem newNodeProblem) {
        NodeRef ref = newNodeProblem.getNoderef();
        List<NodeProblem> nodeProblemList;
        if (nodeProblems.containsKey(ref)) {
            nodeProblemList = nodeProblems.get(ref);
        } else {
            nodeProblemList = new ArrayList<>();
        }
        nodeProblemList.add(newNodeProblem);
        nodeProblems.put(ref, nodeProblemList);
    }

    public void addFileProblem(FileProblem newFileProblem) {
        String path = newFileProblem.getPath();
        List<FileProblem> fileProblemList;
        if (fileProblems.containsKey(path)) {
            fileProblemList = fileProblems.get(path);
        } else {
            fileProblemList = new ArrayList<>();
        }
        fileProblemList.add(newFileProblem);
        fileProblems.put(path, fileProblemList);
    }

    @JsonSerialize(keyUsing = NoderefFieldSerializer.class)
    public Map<NodeRef, List<NodeProblem>> getNodeProblems() {
        return nodeProblems;
    }

    public Map<String, List<FileProblem>> getFileProblems() {
        return fileProblems;
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

    @Override
    public String toString() {
        // Render this report as decently-formatted plaintext (suitable for the email)
        StringBuilder builder = new StringBuilder();
        builder.append("Scanned ").append(getScannedNodes()).append(" nodes in ").append(getRuntime()).append(" ms.\n");
        builder.append("Ran from ").append(getStartTime()).append(" to ").append(getEndTime()).append(".\n");

        if (getNodeProblems().isEmpty()) {
            builder.append("\nNo problems found relating to nodes.\n");
        } else {
            builder.append("\nProblems found relating to noderefs:\n");
            for (Map.Entry<NodeRef, List<NodeProblem>> entry : getNodeProblems().entrySet()) {
                builder.append("\t• ").append(entry.getKey().toString()).append("\n");
                for (NodeProblem np : entry.getValue()) {
                    builder.append("\t\t• ").append(np.getMessage()).append("\n");
                    if (np instanceof NodePropertyProblem) {
                        builder.append("\t\t\t• ").append(((NodePropertyProblem) np).getProperty()).append("\n");
                    }
                }
            }
        }
        if (getFileProblems().isEmpty()) {
            builder.append("\nNo problems found relating to files.\n");
        } else {
            builder.append("\nProblems found relating to files:\n");
            for (Map.Entry<String, List<FileProblem>> entry : getFileProblems().entrySet()) {
                builder.append("\t• ").append(entry.getKey()).append("\n");
                for (FileProblem fp : entry.getValue()) {
                    builder.append("\t\t• ").append(fp.getMessage()).append("\n");
                }
            }
        }
        return builder.toString();
    }
}
