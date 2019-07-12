package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.jobs.ScheduledQuartzJob;
import eu.xenit.care4alf.helpers.NodeHelper;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Thomas S on 11/07/2017.
 */
@Component
@ScheduledQuartzJob(name = "OperationsMetric", group = Monitoring.SCHEDULE_GROUP, cron = "0 0/5 * * * ?", cronProp = "c4a.monitoring.operations.cron")
public class ContentMetric extends AbstractMonitoredSource{
    @Autowired
    private NodeHelper nodeHelper;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private FileFolderService fileFolderService;

    private NodeRef readRef;

    private void init() {
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();
        NodeRef data_dictionary = nodeService.getChildByName(nodeHelper.getCompanyHome(), ContentModel.ASSOC_CONTAINS, "Data Dictionary");
        NodeRef folderRef = nodeHelper.createFolderIfNotExists(data_dictionary, "MetricChecks");
        readRef =  nodeHelper.createDocumentIfNotExists(folderRef, "Metrics test doc");
        try {
            Writer writer = new OutputStreamWriter(fileFolderService.getWriter(readRef).getContentOutputStream(), "UTF-8");
            String content = "General content read and write check";
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        if(readRef == null)
            init();

        Map<String, Long> map = new HashMap<>();
        map.put("operations.read", readTest());
//        map.put("operations.write", writeTest());
//        map.put("operations.search", searchTest());
//        map.put("operations.preview", previewTest());
        return map;
    }

    private long writeTest(){
        Long starttime = System.currentTimeMillis();
        try {
            ContentWriter writer = contentService.getWriter(readRef, ContentModel.PROP_CONTENT, true);
            writer.putContent("An new line of content");
        } catch(Exception e){
            return Long.MAX_VALUE;
        }
        return System.currentTimeMillis() - starttime;
    }

    private long readTest(){
        Long starttime = System.currentTimeMillis();
        try{
            ContentReader reader = contentService.getReader(readRef, ContentModel.PROP_CONTENT);
            if(reader.getContentString() == null){
                return Long.MAX_VALUE;
            }
        } catch(Exception e){
            return Long.MAX_VALUE;
        }
        return System.currentTimeMillis() - starttime;
    }

    private long previewTest(){
        //TODO transformationService
        return -1;
    }

    private long searchTest(){
        Long starttime = System.currentTimeMillis();
        String query = "cm:name:\"Metrics test doc\"";
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
        sp.setQueryConsistency(QueryConsistency.EVENTUAL);
        sp.setQuery(query);
        sp.addStore(new StoreRef("workspace", "SpacesStore"));
        if(this.searchService.query(sp).length() == 0){
            return Long.MAX_VALUE;
        }
        return System.currentTimeMillis() - starttime;
    }
}
