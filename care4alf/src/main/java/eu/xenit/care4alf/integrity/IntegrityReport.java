package eu.xenit.care4alf.integrity;

import java.util.Map;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.NotImplementedException;

public class IntegrityReport {
    public Map<NodeRef,?> getNodeProblems() {
        throw new NotImplementedException();
    }

    public Map<String, ?> getFileProblems() {
        throw new NotImplementedException();
    }
}
