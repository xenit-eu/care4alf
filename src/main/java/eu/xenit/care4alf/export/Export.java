package eu.xenit.care4alf.export;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;
import ucar.nc2.util.HashMapLRU;

import javax.print.DocFlavor;
import java.io.*;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by KevinB on 16/07/2015.
 */
@Component
@Authentication(AuthenticationType.USER)
@Transaction(TransactionType.REQUIRED)
@WebScript(baseUri = "/xenit/care4alf/export", families = {"care4alf"}, description = "Export Alfresco")
public class Export {

    @Autowired
    private SearchService searchService;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private ContentService contentService;
    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private DictionaryService dictionaryService;

    @Autowired
    Nlp nlp;

    @Uri(value="/query", method = HttpMethod.GET)
    public void exportQuery(@RequestParam(required = false) String query,
                            @RequestParam(required = false) String separator,
                            @RequestParam(required = false) String nullValue,
                            @RequestParam(required = false) String documentName,
                            @RequestParam(required = false) String columns,
                            @RequestParam(required = false) String amountDoc,
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

        columns = columns.toLowerCase();
        String[] column = columns.split(",");

        HashMap<String,Boolean> hardcodedNames = new HashMap<String,Boolean>();
        hardcodedNames.put("path",true);
        hardcodedNames.put("text",true);
        hardcodedNames.put("type",true);


        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);

        sp.setQuery(query);
        sp.addStore(new StoreRef("workspace", "SpacesStore"));


        ArrayList<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        ResultSet rs = null;
        int start = 0;
        try {
            do {
                sp.setSkipCount(start);
//                logger.info("Start searching at " + start);
                rs = this.searchService.query(sp);
                nodeRefs.addAll(rs.getNodeRefs());
                start += 1000;
                rs.close();
            }while(rs.getNodeRefs().size() > 0);
        } finally {
            if (rs != null) rs.close();
        }

        response.setContentType("application/CSV");
        response.setContentEncoding(null);
        response.addHeader("Content-Disposition", "inline;filename=" + documentName);
        OutputStream stream = response.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));



        for(int i = 0; i < column.length; i++){
            String el = column[i];
            if(hardcodedNames.containsKey(el))
                writer.write(el);
            else
                writer.write(dictionaryService.getProperty(QName.createQName(el,namespaceService)).getTitle());
            if(i != column.length - 1)
                writer.write(separator);
        }
        writer.newLine();


        int counter = 0;
        for(NodeRef nRef : nodeRefs){
            if(nbDocuments > 0 && counter >= nbDocuments)
                break;
            counter++;
            try {

                String result = "";

                for(int i = 0; i < column.length; i++){
                    try {
                        boolean done = false;
                        String element = column[i];
//                        if ("name".equals(element)) {
//                            result += StringEscapeUtils.escapeCsv((String) this.nodeService.getProperty(nRef, ContentModel.PROP_NAME));
//                            done = true;
//                        }
                        if ("path".equals(element)) {
                            result += StringEscapeUtils.escapeCsv(this.nodeService.getPath(nRef).toDisplayPath(
                                    this.nodeService, permissionService));
                            //                        nodePath = takeFirstN(nodePath,2);
                            done = true;
                        }
                        if ("text".equals(element)) {
                            String text = getText(nRef).replaceAll("[\\W&&[^\\s]]", "");
                            result += text;
                            done = true;
                        }
                        if ("type".equals(element)) {
                            QName type = nodeService.getType(nRef);
                            result += dictionaryService.getType(type).getTitle();
                            done = true;
                        }
                        if (!done) {
                            PropertyDefinition prop = dictionaryService.getProperty(QName.createQName(element,namespaceService));
                            result += StringEscapeUtils.escapeCsv(nodeService.getProperty(nRef,prop.getName()).toString());
                        }
                    }
                    catch(RuntimeException e){
                        result += nullValue;
                    }
                    if (i < column.length - 1)
                        result += separator;
                }

                String str = result;
                writer.write(str);
                writer.newLine();
            }
            catch (IOException e){
                counter--;
            }
        }
        writer.close();

    }


//        file export code
//        https://forums.alfresco.com/forum/developer-discussions/alfresco-share-development/changing-file-name-download-05152012-1352
    @Uri("export/{nodeRef}")
     public void export(final @UriVariable NodeRef nodeRef, final WebScriptResponse response) throws IOException {
        String query = "";


        NodeRef parentRef = this.nodeService.getPrimaryParent(nodeRef).getParentRef();
        String qnamepath = nodeService.getPath(nodeRef).toPrefixString(this.namespaceService);
        query += String.format("PATH:\"%s//*\"",qnamepath);

        query += "AND TYPE:\"cm:content\"";

        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);

        sp.setQuery(query);
//        sp.addSort(SearchParameters.SortDefinition.SortType.SCORE); //is default already
//        sp.addSort("score", false);
        sp.addStore(new StoreRef("workspace", "SpacesStore"));


//        ResultSet rs = searchService.query(sp);

        ArrayList<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        ResultSet rs = null;
        int start = 0;
        try {
            do {
                sp.setSkipCount(start);
//                logger.info("Start searching at " + start);
                rs = this.searchService.query(sp);
                nodeRefs.addAll(rs.getNodeRefs());
                start += 1000;
                rs.close();
            }while(rs.getNodeRefs().size() > 0);
        } finally {
            if (rs != null) rs.close();
        }

        response.setContentType("application/CSV");
        response.setContentEncoding(null);
        response.addHeader("Content-Disposition", "inline;filename=test.csv");
        OutputStream stream = response.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));

        writer.write("name");
        writer.write(",");
        writer.write("creator");
        writer.write(",");
        writer.write("text");
        writer.write(",");
        writer.write("path");

        writer.newLine();

        int counter = 0;
        for(NodeRef nRef : nodeRefs){
//            if(counter >= 100)
//                break;
            counter++;
            try {
                String nodeName = (String) this.nodeService.getProperty(nRef, ContentModel.PROP_NAME);
                String nodePath = this.nodeService.getPath(nRef).toDisplayPath(
                        this.nodeService, permissionService);
                String creator = nodeService.getProperty(nRef, QName.createQName("{http://www.alfresco.org/model/content/1.0}creator")).toString();


                nodePath = takeFirstN(nodePath,2);
                //                String text = getText(nRef).replaceAll("[\\W&&[^\\s]]", "");
                String terms = getTerms(nRef);

                writer.write(StringEscapeUtils.escapeCsv(nodeName));
                writer.write(",");
                writer.write(StringEscapeUtils.escapeCsv(creator));
                writer.write(",");

                //                writer.write(StringEscapeUtils.escapeCsv(text));
                writer.write(StringEscapeUtils.escapeCsv(terms));
                writer.write(",");


                writer.write(StringEscapeUtils.escapeCsv(nodePath));
                writer.newLine();
            }
            catch (Exception e){
                //                writer.write("Error:");
                //                writer.write(nRef.toString());
                //                writer.newLine();
                //                writer.write(e.toString());
                ////                writer.write("\n");
                //                writer.newLine();
                counter--;
            }
        }
        writer.close();

    }

    private String takeFirstN(String nodePath, int N) {
        String[] split = nodePath.split("/");

        if(split.length < 4)
            return nodePath;


        String result = split[2];
        for(int k = 1;k < N; k++) {
            result += "/" + split[2+k];
        }
        return result;
    }



    private String getTerms(NodeRef nodeRef) throws SQLException, EncoderException, JSONException, IOException {

        TreeMap<String, Double> tfidfs = nlp.getTfIdfs(nodeRef);
        List<String> terms = new ArrayList<String>();
        terms.addAll(tfidfs.keySet());

        String conTerms = "";
        int nbTerms = 40;
        for(int i = 0; i < nbTerms; i++){
            conTerms += " " + terms.get(i);
        }


        return conTerms;
    }


    public String getText(NodeRef nodeRef)
    {
        ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);

        TransformationOptions options = new TransformationOptions();
        options.setSourceNodeRef(nodeRef);
        List<ContentTransformer> transformers = contentService.getActiveTransformers(reader.getMimetype(), reader.getSize(), MimetypeMap.MIMETYPE_TEXT_PLAIN, options);

        if(transformers.size() == 0){
            //When there are no transformers, there is no text.
            return "";
        }

        ContentTransformer transformer = transformers.get(0);
        ContentWriter writer = contentService.getTempWriter();
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");


        transformer.transform(reader, writer);

        ContentReader textReader = writer.getReader();
        return (textReader != null && textReader.exists()) ? textReader.getContentString() : "";
    }


    @Uri("exportMap/{nodeRef}")
    public void exportMap(final @UriVariable NodeRef nodeRef, @RequestParam(required = false) Integer nb, final WebScriptResponse response) throws IOException {

        int nbRefs = 100;
        if(nb != null)
            nbRefs = nb;

        String query = "";


        NodeRef parentRef = this.nodeService.getPrimaryParent(nodeRef).getParentRef();
        String qnamepath = nodeService.getPath(nodeRef).toPrefixString(this.namespaceService);
        query += String.format("PATH:\"%s//*\"",qnamepath);

        query += "AND TYPE:\"cm:content\"";

        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);

        sp.setQuery(query);
        sp.addStore(new StoreRef("workspace", "SpacesStore"));


        ArrayList<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        ResultSet rs = null;
        int start = 0;
        try {
            do {
                sp.setSkipCount(start);
//                logger.info("Start searching at " + start);
                rs = this.searchService.query(sp);
                nodeRefs.addAll(rs.getNodeRefs());
                start += 1000;
                rs.close();
            }while(rs.getNodeRefs().size() > 0);
        } finally {
            if (rs != null) rs.close();
        }

        response.setContentType("application/CSV");
        response.setContentEncoding(null);
        response.addHeader("Content-Disposition", "inline;filename=testMap.csv");
        OutputStream stream = response.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));

        ArrayList<String> listOfTerms = new ArrayList<String>();
        Map<NodeRef,HashMap<Integer,Boolean>> mapping = getMapping(nodeRefs.subList(0,nbRefs), listOfTerms);

        int nbTerms = listOfTerms.size();

        writer.write("name");
        for(String term : listOfTerms) {
            writer.write(",");
            writer.write(term);
        }

        if(listOfTerms.size() == 0)
            writer.write(",no terms");

        writer.write(",");
        writer.write("path");

//        writer.write(new Integer(mapping.keySet().size()).toString());

        writer.newLine();

        int counter = 0;
        for(NodeRef nRef : nodeRefs){
            if(counter >= nbRefs)
                break;
            counter++;
            try {
                String nodeName = (String) this.nodeService.getProperty(nRef, ContentModel.PROP_NAME);
                String nodePath = this.nodeService.getPath(nRef).toDisplayPath(
                        this.nodeService, permissionService);
                nodePath = takeFirstN(nodePath,2);

                HashMap<Integer,Boolean> termMap = mapping.get(nRef);

                writer.write(StringEscapeUtils.escapeCsv(nodeName));
                writer.write(",");

                for(int i = 0; i < nbTerms; i++){
                    boolean b = false;
                    if(termMap.containsKey(i))
                        b = termMap.get(i);
                    if(b)
                        writer.write("T");
                    else
                        writer.write("F");
                    writer.write(",");
                }

                writer.write(StringEscapeUtils.escapeCsv(nodePath));
                writer.newLine();
            }
            catch (Exception e){
//                counter--;
            }
        }
        writer.close();

    }

    private Map<NodeRef,HashMap<Integer,Boolean>> getMapping( List<NodeRef> nodeRefs, ArrayList<String> termsList){
        HashMap<NodeRef,HashMap<Integer,Boolean>> nodeMap = new HashMap<NodeRef,HashMap<Integer,Boolean>>();
        HashMap<String,Integer> termMap = new HashMap<String,Integer>();

        int counter = 0;
        for(NodeRef nRef: nodeRefs){
            try{
                HashMap<Integer,Boolean> hasTerms = new HashMap<Integer,Boolean>();
                nodeMap.put(nRef, hasTerms);
                for(String term: getTermsList(nRef)){
                    if(termMap.containsKey(term)){
                        Integer index = termMap.get(term);
                        hasTerms.put(index, true);
                    }
                    else{
                        termMap.put(term,counter);
                        hasTerms.put(counter, true);
                        termsList.add(term);
                        counter++;
                    }
                }
                if(hasTerms.isEmpty()){
                    hasTerms.put(termsList.size(), true);
                    termsList.add("isEmpty");

                }

            }
            catch(Exception e){
//                termsList.add("Exception:"+ e.getMessage());
            }
        }
        if(termsList.isEmpty())
            termsList.addAll(termMap.keySet());

        return nodeMap;
    }

    private List<String> getTermsList(NodeRef nodeRef) throws SQLException, EncoderException, JSONException, IOException {

        TreeMap<String, Double> tfidfs = nlp.getTfIdfs(nodeRef);
        List<String> terms = new ArrayList<String>();
        terms.addAll(tfidfs.keySet());

        int nbTerms = 40;

        if(terms.size() == 0)
            terms.add("emptyException123");

        return terms.subList(0, nbTerms);
    }


}
