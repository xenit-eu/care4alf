package eu.xenit.care4alf.integrity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntegrityReportSummary {
    public int scannedNodes;
    public Date startTime;
    public Date endTime;
    public long runtime;
    @JsonProperty("problemCounts")
    public Map<String, Integer> problemFreqMap;

    public IntegrityReportSummary(IntegrityReport report) {
        this.scannedNodes = report.getScannedNodes();
        this.startTime = report.getStartTime();
        this.endTime = report.getEndTime();
        this.runtime = report.getRuntime();

        this.problemFreqMap = new HashMap<>();

        for(List<FileProblem> problems : report.getFileProblems().values()) {
            for (FileProblem problem : problems) {
                incrementFrequency(problemFreqMap, problem);
            }
        }
        for(List<NodeProblem> problems : report.getNodeProblems().values()) {
            for (NodeProblem problem : problems) {
                incrementFrequency(problemFreqMap, problem);
            }
        }
    }

    private void incrementFrequency(Map<String, Integer> frequencies, Problem problem) {
        String name = problem.getClass().getSimpleName();
        Integer count = frequencies.get(name);
        if (count != null) {
            frequencies.put(name, count + 1);
        } else {
            frequencies.put(name, 1);
        }
    }
}
