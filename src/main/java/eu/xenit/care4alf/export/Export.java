package eu.xenit.care4alf.export;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import eu.xenit.care4alf.helpers.NodeHelper;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by KevinB on 16/07/2015.
 */
@Component
@Authentication(AuthenticationType.ADMIN)
@WebScript(baseUri = "/xenit/care4alf/export", families = {"care4alf"}, description = "Export Alfresco")
public class Export {
    private final Logger logger = LoggerFactory.getLogger(Export.class);

    @Autowired
    private SearchService searchService;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private NodeHelper nodeHelper;
    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private ContentService contentService;
    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private DictionaryService dictionaryService;
    @Autowired
    private FileFolderService fileFolderService;
    @Autowired
    private RetryingTransactionHelper retryingTransactionHelper;

    @Uri(value="/query", method = HttpMethod.GET)
    @Transaction(readOnly = false)
    public void exportQuery(@RequestParam(required = false) String query,
                            @RequestParam(required = false) String separator,
                            @RequestParam(required = false) String nullValue,
                            @RequestParam(required = false) String documentName,
                            @RequestParam(required = false) String columns,
                            @RequestParam(required = false) String amountDoc,
                            @RequestParam(required = false) String path,
                            @RequestParam(required = false, defaultValue = "false") boolean localSave,
                            final WebScriptResponse response) throws IOException{
        if(query == null)
            query = "PATH:\"/app:company_home/cm:Projects_x0020_Home//*\" AND TYPE:\"cm:content\"";
        if(columns == null)
            columns = "cm:name,path";
        if(separator == null)
            separator = ",";
        if(nullValue == null)
            nullValue = "null";
        if(documentName == null)
            documentName = "no_name.csv";
        if(amountDoc == null)
            amountDoc = "-1";
        int nbDocuments = Integer.parseInt(amountDoc);

        String[] column = columns.split(",");

        HashMap<String,Boolean> hardcodedNames = new HashMap<String,Boolean>();
        hardcodedNames.put("path",true);
        hardcodedNames.put("text",true);
        hardcodedNames.put("type",true);
        hardcodedNames.put("noderef",true);

        List<String> pathElements = Arrays.asList(StringUtils.split(path, '/'));
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);

        sp.setQuery(query);
        sp.addStore(new StoreRef("workspace", "SpacesStore"));


        ArrayList<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        ResultSet rs = null;
        int start = 0;
        logger.info("Fetching noderefs");
        long startTime = System.currentTimeMillis();
        try {
            do {
                sp.setSkipCount(start);
                rs = this.searchService.query(sp);
                nodeRefs.addAll(rs.getNodeRefs());
                start += 1000;
                rs.close();
                logger.info("#noderefs: " + nodeRefs.size());
            }while(rs.getNodeRefs().size() > 0 && (nodeRefs.size() < nbDocuments || nbDocuments == -1));//TODO: nodeRefs.size <= nbDocuments
        } finally {
            if (rs != null) rs.close();
        }





        Writer outputStreamWriter;

        if(localSave){
            NodeRef parentFolder;
            try {
                parentFolder = fileFolderService.resolveNamePath(nodeHelper.getCompanyHome(), pathElements).getNodeRef();
            } catch(Exception e){
                parentFolder = retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>() {
                    @Override
                    public NodeRef execute() throws Throwable {
                        return nodeHelper.createFolderIfNotExists(nodeHelper.getCompanyHome(),"CSVExports");
                    }
                }, false, true);
            }
            final NodeRef finalParentFolder = parentFolder;
            final String finalDocumentName = documentName;
            NodeRef ref = retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>() {
                @Override
                public NodeRef execute() throws Throwable {
                    return nodeHelper.createDocument(finalParentFolder, finalDocumentName);
                }
            }, false, true);
            ContentWriter contWriter = contentService.getWriter(ref, ContentModel.PROP_CONTENT, false);
            contWriter.setMimetype(MimetypeMap.MIMETYPE_TEXT_CSV);
            outputStreamWriter = new OutputStreamWriter(contWriter.getContentOutputStream(),"UTF-8");
        } else{
            response.setContentType("application/CSV");
            response.setContentEncoding(null);
            response.addHeader("Content-Disposition", "inline;filename=" + documentName);
            outputStreamWriter = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
        }

        for(int i = 0; i < column.length; i++){
            String el = column[i];
            if(hardcodedNames.containsKey(el))
                outputStreamWriter.write(el);
            else
                outputStreamWriter.write(dictionaryService.getProperty(QName.createQName(el,namespaceService)).getTitle());
            if(i != column.length - 1)
                outputStreamWriter.write(separator);
        }
        outputStreamWriter.write("\n");

//        for(int i = 0; i < column.length; i++){
//            String el = column[i];
//            if(hardcodedNames.containsKey(el))
//                writer.write(el);
//            else
//                writer.write(dictionaryService.getProperty(QName.createQName(el,namespaceService)).getTitle());
//            if(i != column.length - 1)
//                writer.write(separator);
//        }
//        writer.newLine();



        logger.info("Start writing csv");
        int n=1;
        for(NodeRef nRef : nodeRefs){
            try {
                StringBuilder result = new StringBuilder();
                for(int i = 0; i < column.length; i++){
                    try {
                        String element = column[i];
                        if ("path".equals(element)) {
                            result.append(StringEscapeUtils.escapeCsv(this.nodeService.getPath(nRef).toDisplayPath(
                                    this.nodeService, permissionService)));
                        }
                        else if ("type".equals(element)) {
                            QName type = nodeService.getType(nRef);
                            result.append(dictionaryService.getType(type).getTitle());
                        }
                        else if ("noderef".equals(element)) {
                            result.append(nRef);
                        }
                        else {
                            result.append(StringEscapeUtils.escapeCsv(nodeService.getProperty(nRef,QName.createQName(element, namespaceService)).toString()));
                        }
                    }
                    catch(RuntimeException e){
                        result.append(nullValue);
                    }
                    if (i < column.length - 1)
                        result.append(separator);
                }
                String str = result.toString();
                outputStreamWriter.write(str);
                outputStreamWriter.write("\n");
                n++;
                if(n%1000==0)
                    logger.info("#noderefs written to csv: " + n);
            }
            catch (IOException e){
                logger.debug("Exception caught: {}", e.getLocalizedMessage());
            }
        }
        outputStreamWriter.flush();
        outputStreamWriter.close();
        long endTime   = System.currentTimeMillis();
        long duration = endTime - startTime;
        logger.info("Duration in seconds: " + duration/1000d);
        logger.info((nodeRefs.size()/(duration/1000d)) + " docs/s");
    }

}