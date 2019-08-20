package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.stereotype.Component;

@Component
@WebScript(baseUri = "/xenit/care4alf/audit", families = {"care4alf"}, description = "Audit")
public class Audit {
    @Autowired
    AuditService auditService;

    private final static Logger logger = LoggerFactory.getLogger(Audit.class);

    @Uri(value = "/{application}/{id}", method = HttpMethod.DELETE)
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
}
