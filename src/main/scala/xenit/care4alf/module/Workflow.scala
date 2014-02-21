package xenit.care4alf.module

import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.service.namespace.NamespaceService
import xenit.care4alf.web.{JsonHelper, Json}
import com.github.dynamicextensionsalfresco.webscripts.annotations._

import scala.collection.JavaConversions._
import org.springframework.stereotype.Component
import org.alfresco.service.cmr.workflow.{WorkflowTaskQuery, WorkflowInstance, WorkflowService}
import com.typesafe.scalalogging.slf4j.Logging
import org.alfresco.service.cmr.search.SearchService
import org.alfresco.service.cmr.repository.{NodeService, StoreRef}
import org.alfresco.model.ContentModel
import xenit.care4alf.alfresco.{HasNamespaceService, HasNodeService}
import org.springframework.extensions.webscripts.WebScriptResponse

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/workflow", families = Array("care4alf"), description = "Workflow tools")
@Authentication(AuthenticationType.ADMIN)
class Workflow extends Json with Logging with HasNodeService with HasNamespaceService {
    @Autowired var workflowService: WorkflowService = null
    @Autowired var searchService: SearchService = null

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
        logger.warn(s"Delete workflow definition and instances for id $workflowId.")

        val definition = workflowService.getDefinitionById(workflowId)
        if (definition != null) {
            for (active <- workflowService.getActiveWorkflows(workflowId)) {
                workflowService.cancelWorkflow(active.getId)
            }
            for (complete <- workflowService.getCompletedWorkflows(workflowId)) {
                workflowService.deleteWorkflow(complete.getId)
            }
            val name = definition.getName
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
        } else {
            logger.error(s"Could not find workflow definition $workflowId.")
        }
    }

    @Uri(value = Array("/instances/active"), defaultFormat = "json")
    def activeInstances(@Attribute jsonHelper: JsonHelper) {
        val json = jsonHelper.json
        json.array()
        for (instance <- workflowService.getActiveWorkflows.filter(_.getDefinition != null)) {
            val tasks = getTasksForInstance(instance)

            val initiator = instance.getInitiator()
            val initiatorUsername = if (nodeService.exists(initiator)) initiator(ContentModel.PROP_USERNAME).asInstanceOf[String] else "-"
            json.`object`()
                .key("description").value(instance.getDefinition.getDescription)
                .key("id").value(instance.getId)
                .key("initiator").value(initiatorUsername)
                .key("start").value(instance.getStartDate.getTime)
                .key("files").array()
                for (assoc <- nodeService.getChildAssocs(instance.getWorkflowPackage)) {
                    json.value(assoc.getChildRef.path)
                }
                json.endArray()
            json.key("tasks").array()
            for (task <- tasks) {
                json.`object`()
                    .key("id").value(task.getId)
                    .key("description").value(task.getDescription)
                    .key("properties").`object`()
                     for (prop <- task.getProperties) {
                         json.key(prop._1.toString).value(prop._2)
                     }
                     json.endObject()
                .endObject()
            }
            json.endArray()
            
            json.endObject()
        }
        json.endArray()
    }

    def getTasksForInstance(instance: WorkflowInstance) = {
        val query = new WorkflowTaskQuery
        query.setActive(true)
        query.setProcessId(instance.getId)
        workflowService.queryTasks(query)
    }

    @Uri(value = Array("/instances/{id}/cancel"), method = HttpMethod.DELETE)
    def cancelWorkflow(@UriVariable("id") id: String) {
        workflowService.cancelWorkflow(id)
    }

    @Uri(value = Array("/instances/{id}/delete"), method = HttpMethod.DELETE)
    def deleteWorkflow(@UriVariable("id") id: String) {
        workflowService.deleteWorkflow(id)
    }

    @ExceptionHandler(Array(classOf[Exception]))
    def exception(exception: Exception, response: WebScriptResponse) {
        logger.error("Workflow error", exception)
        response.setStatus(500)
        response.getWriter.write(exception.getMessage)
    }
}
