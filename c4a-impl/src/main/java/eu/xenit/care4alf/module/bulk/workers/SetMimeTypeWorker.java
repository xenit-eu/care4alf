package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * Bulk order to change/enforce mimetype on selection result.
 *
 * Source of failure for guessMimetype(fileName, ContentReader) is still unknown
 *
 * Created by Robrecht on 5/16/18.
 */
@Component
@Worker(action = "setmimetype", parameterNames = {"MimeType", "ConversionOption"})
public class SetMimeTypeWorker extends AbstractWorker {

    public SetMimeTypeWorker() {
        super(null);
    }

    public SetMimeTypeWorker(JSONObject parameters) {
        super(parameters);

    }

    public void process(final NodeRef nodeRef) throws Throwable {
        ContentData contentData = (ContentData) this.nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
        ContentData modifiedContentData = null;
        String conversionOption = this.parameters.getString("ConversionOption");
        String mimeType = "";
        String fileName = "";
        switch(conversionOption){
            case "force":
                mimeType = this.parameters.getString("MimeType");
                modifiedContentData = ContentData.setMimetype(contentData, mimeType);
                break;
            case "simpleguess":
                fileName = (String) this.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
                mimeType = this.mimetypeService.guessMimetype(fileName);
                modifiedContentData = ContentData.setMimetype(contentData, mimeType);
                break;
            // guessMimetype(fileName, ContentReader) seems to provide incorrect guesses. It is unclear whether the cause is the method itself, or the contentReaders
            default:
                modifiedContentData = contentData;
                break;
        }
        this.nodeService.setProperty(nodeRef, ContentModel.PROP_CONTENT, modifiedContentData);
    }

}