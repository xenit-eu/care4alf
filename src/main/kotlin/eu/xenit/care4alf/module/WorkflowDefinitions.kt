package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import org.springframework.stereotype.Component
import eu.xenit.care4alf.json
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod
import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.service.cmr.workflow.WorkflowService
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam
import org.alfresco.service.cmr.repository.StoreRef
import org.alfresco.service.cmr.search.SearchService
import org.alfresco.model.ContentModel
import org.alfresco.service.cmr.repository.NodeService

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/workflow/definitions", families = arrayOf("care4alf"), description = "Workflow definitions")
Authentication(AuthenticationType.ADMIN)
public class WorkflowDefinitions @Autowired constructor(
        private val workflowService: WorkflowService,
        private val nodeService: NodeService,
        private val searchService: SearchService
    ) {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)

    Uri(defaultFormat = "json", method = HttpMethod.GET)
    fun list() = json {
        iterable(workflowService.getAllDefinitions()) { definition ->
            obj {
                entry("id", definition.getId())
                entry("name", definition.getName())
                entry("version", definition.getVersion())
                entry("description", definition.getDescription())
            }
        }
    }

    Uri(defaultFormat = "json", method = HttpMethod.DELETE)
    fun delete(RequestParam workflowId: String) {
        logger.warn("Delete workflow definition and instances for id $workflowId.")

        val definition = workflowService.getDefinitionById(workflowId)
        if (definition != null) {
            for (active in workflowService.getActiveWorkflows(workflowId)) {
                workflowService.cancelWorkflow(active.getId())
            }
            for (complete in workflowService.getCompletedWorkflows(workflowId)) {
                workflowService.deleteWorkflow(complete.getId())
            }
            val name = definition.getName()
            val query = "@bpm\\:definitionName:\"$name\""
            val definitionFiles = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, query)
            if (definitionFiles.length() > 0) {
                for (defFile in definitionFiles) {
                    nodeService.addAspect(defFile.getNodeRef(), ContentModel.ASPECT_TEMPORARY, null)
                    nodeService.deleteNode(defFile.getNodeRef())
                }
            } else {
                workflowService.undeployDefinition(workflowId)
            }
        } else {
            throw IllegalArgumentException("Could not find defintion $workflowId.")
        }
    }
}