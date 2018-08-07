package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.actions.annotations.ActionMethod;
import com.github.dynamicextensionsalfresco.actions.annotations.ActionParam;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ExampleAction {
    public static final String SET_TITLE_ACTION = "change-title";
    public static final String NEW_TITLE= "newtitle";
    @Autowired
    private NodeService nodeService;

    @ActionMethod(SET_TITLE_ACTION)
    public void changeTitle(final NodeRef nodeRef, @ActionParam(NEW_TITLE) String newtitle) {
        if (nodeService.exists(nodeRef)) {
            if (newtitle == null || newtitle.equals("")) {
                newtitle = "Placeholder example title";
            }
            nodeService.setProperty(nodeRef, ContentModel.PROP_TITLE, newtitle);
        }
    }

}
