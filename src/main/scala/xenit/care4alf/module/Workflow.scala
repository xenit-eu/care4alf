package xenit.care4alf.module

import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.service.namespace.NamespaceService
import xenit.care4alf.web.{JsonHelper, Json}
import com.github.dynamicextensionsalfresco.webscripts.annotations._

import scala.collection.JavaConversions._
import org.springframework.stereotype.Component
import org.alfresco.service.cmr.workflow.{WorkflowInstance, WorkflowService}
import com.typesafe.scalalogging.slf4j.Logging
import org.alfresco.service.cmr.search.SearchService
import org.alfresco.service.cmr.repository.{NodeService, StoreRef}
import org.alfresco.model.ContentModel

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/workflow", families = Array("care4alf"), description = "Workflow tools")
class Workflow @Autowired() (namespaceService: NamespaceService) extends Json with Logging
{
    @Autowired var workflowService: WorkflowService = null
    @Autowired var searchService: SearchService = null
    @Autowired var nodeService: NodeService = null

    @Uri(defaultFormat = "json", method = HttpMethod.GET)
    def list(@Attribute jsonHelper: JsonHelper) {
        val json = jsonHelper.json
        json.array()
        for (definition <- workflowService.getAllDefinitions) {
            json.`object`()
                .key("id").value(definition.getId)
                .key("name").value(definition.getName)
                .key("version").value(definition.getVersion)
                .key("description").value(definition.getDescription)
            .endObject()
        }
        json.endArray()
    }

    @Uri(value = Array("/{workflowId}"), defaultFormat = "json", method = HttpMethod.DELETE)
    def deleteNameless(@UriVariable workflowId: String) {
        for (list <- Seq(workflowService.getCompletedWorkflows,workflowService.getActiveWorkflows)) {
            if (list != null) {
                list.filter((wf) => wf.getDefinition != null && wf.getDefinition.getId == workflowId)
                    .foreach((workflow) => workflowService.cancelWorkflow(workflow.getId))
            }
        }

        val name = workflowService.getDefinitionById(workflowId).getName
        val query = "@bpm\\:definitionName:\"" + name + "\""
        val definitionFiles = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, query)
        if (definitionFiles.length() > 0) {
            for (defFile <- definitionFiles) {
                nodeService.addAspect(defFile.getNodeRef, ContentModel.ASPECT_TEMPORARY, null)
                nodeService.deleteNode(defFile.getNodeRef)
            }
        } else {
            workflowService.undeployDefinition(workflowId)
        }
    }
}
