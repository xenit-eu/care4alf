package eu.xenit.care4alf.module.bulk;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by stan on 9/6/16.
 */
public class FileWorkProvider implements BatchProcessWorkProvider<NodeRef> {
    private final static Logger LOGGER = LoggerFactory.getLogger(FileWorkProvider.class);

    private ServiceRegistry serviceRegistry;

    private int batchSize;
    private int skipCount;
    private boolean cancel;

    private InputStream content;
    private List<NodeRef> nodeRefs;

    public FileWorkProvider(ServiceRegistry serviceRegistry, InputStream content, int batchSize) {
        this.serviceRegistry = serviceRegistry;
        this.batchSize = batchSize;
        this.content = content;
    }

    @Override
    public int getTotalEstimatedWorkSize() {
        if(this.nodeRefs == null)
            return -1;
        return this.nodeRefs.size();
    }

    @Override
    public Collection<NodeRef> getNextWork() {
        if(this.cancel){
            return Collections.emptyList();
        }

        if(this.nodeRefs == null){
            getAllNodes();
        }

        if(skipCount >= this.nodeRefs.size())
            return Collections.emptyList();

        int from = skipCount;
        int to = Math.min(skipCount + this.batchSize, this.nodeRefs.size());
        skipCount += batchSize;

        return this.nodeRefs.subList(from, to);
    }

    private void getAllNodes(){
        try {
            if (nodeRefs == null){
                nodeRefs = new ArrayList<NodeRef>();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(content));

            String line;
            while((line = reader.readLine()) != null){
                NodeRef node = new NodeRef(line);
                if (serviceRegistry.getNodeService().exists(node)){
                    nodeRefs.add(node);
                }
                else {
                    LOGGER.warn("NodeRef " + line + " does not exist, skipping from work list.");
                }

            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void cancel(){
        this.cancel = true;
    }
}
