package eu.xenit.care4alf.audit;

import static eu.xenit.care4alf.JsonKt.jsonJava;
import static eu.xenit.care4alf.helpers.UtilHelper.codepointToString;

import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.namespace.NamespaceService;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@WebScript(baseUri = "/xenit/care4alf/audit", families = {"care4alf"}, description = "Audit")
public class Audit {
    @Autowired
    AuditService auditService;

    @Autowired
    NodeService nodeService;

    @Autowired
    NamespaceService namespaceService;

    private static final Logger logger = LoggerFactory.getLogger(Audit.class);
    public static final String PATH = "/alfresco-access/transaction/path";

    @Uri(value = "/node/{application}", method = HttpMethod.GET, defaultFormat = MediaType.APPLICATION_JSON_VALUE)
    public Resolution getAuditFilteredByNode(@UriVariable String application,
            @RequestParam NodeRef noderef,
            @RequestParam(required = false) Long fromTime,
            @RequestParam(required = false) Long fromId,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) Boolean forward,
            @RequestParam(required = false, defaultValue = "1000") Integer limit) {
        AuditQueryParameters params = new AuditQueryParameters();
        params.setApplicationName(application);
        params.addSearchKey(PATH, getNodePath(noderef));

        params.setFromTime(fromTime);
        params.setFromId(fromId);
        params.setUser(user);
        params.setForward(forward);

        DefaultAuditCollector callback = new DefaultAuditCollector();
        auditService.auditQuery(callback, params, limit);
        List<Entry> entries = callback.getEntries();

        return jsonJava((writer) -> {
            writer.object()
                    .key("count").value(entries.size())
                    .key("entries").array();
            // loop over each entry
            for (Entry e : entries) {
                writer.object()
                        .key("id").value(e.getId())
                        .key("application").value(e.getApplication())
                        .key("user").value(e.getUser())
                        .key("time").value(e.getTime())
                        .key("values").value(e.getValues())
                        .endObject();
            }
            // write closing braces for array and obj
            writer.endArray().endObject();
        });
    }

    @Uri(value = "/id/{application}/{id}", method = HttpMethod.DELETE)
    public Resolution deleteEntry(@UriVariable String application, @UriVariable long id) {
        final List<Long> auditEntryIds = new ArrayList<>();

        AuditQueryParameters params = new  AuditQueryParameters();
        AuditQueryCallback callback = new AuditQueryCallback() {
            @Override
            public boolean valuesRequired() {
                return false;
            }

            @Override
            public boolean handleAuditEntry(Long entryId, String applicationName, String user, long time,
                    Map<String, Serializable> values) {
                auditEntryIds.add(entryId);
                return false;
            }

            @Override
            public boolean handleAuditEntryError(Long entryId, String errorMsg, Throwable error) {
                logger.error(error.getLocalizedMessage());
                return true;
            }
        };

        params.setFromId(id);
        // Alfresco forgets the +1...
        params.setToId(id + 1);
        auditService.auditQuery(callback, params, 1);
        logger.debug("Query for audit nodes returned {} result(s).", auditEntryIds.size());

        if(auditEntryIds.size() > 0) {
            final int deleted = auditService.clearAudit(auditEntryIds);
            logger.info("Deleted audit nodes {} {}", application, auditEntryIds);
            return new JsonWriterResolution() {
                @Override
                protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                    jsonWriter.object();
                    jsonWriter.key("deleted");
                    jsonWriter.value(deleted);
                    jsonWriter.endObject();
                }
            };
        } else {
            // Not found
            throw new WebScriptException(Status.STATUS_NOT_FOUND, "audit.err.entry.id.notfound", id);
        }
    }

    public String getNodePath(NodeRef noderef) {
        Path path = nodeService.getPath(noderef);
        // The qname path contains unicode escape sequences in the form of _x\d{4}_, e.g. _x0020_ for a space
        // The audit api's filter-by-value also uses qname paths but does NOT include these escape sequences
        // So we replace all these sequences in the path by their unescaped forms before continuing with the audit
        String escapedQnamePath = path.toPrefixString(namespaceService);
        String qnamePath = null;

        Pattern p = Pattern.compile("_x(\\d{4})_");
        Matcher matcher = p.matcher(escapedQnamePath);
        while(matcher.find()) {
            // find the next escape sequence, parse the base-16 number that represents the codepoint
            int codePoint = Integer.parseInt(matcher.group(1), 16);
            // convert the codepoint to String and replace the match we just found
            qnamePath = matcher.replaceFirst(codepointToString(codePoint));
            // re-initialize the matcher on the new string so we don't keep replacing the same bit
            matcher = p.matcher(qnamePath);
        }
        logger.debug("Restricting to value = {}", qnamePath);
        return qnamePath;
    }
}
