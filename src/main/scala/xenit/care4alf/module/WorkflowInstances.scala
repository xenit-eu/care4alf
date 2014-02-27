package xenit.care4alf.module

import org.springframework.beans.factory.annotation.Autowired
import xenit.care4alf.web.{JsonHelper, Json}
import com.github.dynamicextensionsalfresco.webscripts.annotations._

import scala.collection.JavaConversions._
import org.springframework.stereotype.Component
import org.alfresco.service.cmr.workflow.{WorkflowTaskQuery, WorkflowInstance, WorkflowService}
import com.typesafe.scalalogging.slf4j.Logging
import org.alfresco.model.ContentModel
import xenit.care4alf.alfresco.{HasNamespaceService, HasNodeService}

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/workflow/instances", families = Array("care4alf"), description = "Workflow instances")
@Authentication(AuthenticationType.ADMIN)
class WorkflowInstances extends Json with Logging with HasNodeService with HasNamespaceService with RestErrorHandling {
    @Autowired var workflowService: WorkflowService = null

    @Uri(value = Array("/active"), defaultFormat = "json")
    def activeInstances(@Attribute jsonHelper: JsonHelper, @RequestParam(defaultValue = "includeTasks") includeTasks: Boolean) {
        val json = jsonHelper.json
        json.array()
        for (instance <- workflowService.getActiveWorkflows.filter(_.getDefinition != null)) {
            instanceToJson(instance, jsonHelper, includeTasks)
        }
        json.endArray()
    }

    @Uri(value = Array("/find/task/{taskid}"), defaultFormat = "json")
    def instanceByTask(@Attribute jsonHelper: JsonHelper, @UriVariable taskid: String) {
        val json = jsonHelper.json
        val query = new WorkflowTaskQuery
        query.setTaskId(taskid)
        json.array()
        for (task <- workflowService.queryTasks(query)) {
            instanceToJson(task.getPath.getInstance(), jsonHelper, includeTasks = true)
        }
        json.endArray()
    }

    @Uri(value = Array("/{instanceid}"), defaultFormat = "json")
    def instanceById(@Attribute jsonHelper: JsonHelper, @UriVariable instanceid: String) {
        val json = jsonHelper.json
        json.array()
        instanceToJson(workflowService.getWorkflowById(instanceid), jsonHelper, true)
        json.endArray()
    }


    def instanceToJson(instance: WorkflowInstance, jsonHelper: JsonHelper, includeTasks: Boolean) {
        val json = jsonHelper.json
        val initiator = instance.getInitiator
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
        if (includeTasks) {
            json.key("tasks")
            tasksForInstance(jsonHelper, instance.getId)
        }
        json.endObject()
    }

    @Uri(value = Array("/{workflowid}/tasks"), defaultFormat = "json")
    def tasksForInstance(@Attribute jsonHelper: JsonHelper, @UriVariable workflowid: String) {
        val json = jsonHelper.json
        val tasks = getTasksForInstance(workflowService.getWorkflowById(workflowid))
        json.array()
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
    }

    def getTasksForInstance(instance: WorkflowInstance) = {
        val query = new WorkflowTaskQuery
        query.setActive(true)
        query.setProcessId(instance.getId)
        workflowService.queryTasks(query)
    }

    @Uri(value = Array("/{id}/cancel"), method = HttpMethod.DELETE)
    def cancelWorkflow(@UriVariable("id") id: String) {
        workflowService.cancelWorkflow(id)
    }

    @Uri(value = Array("/{id}/delete"), method = HttpMethod.DELETE)
    def deleteWorkflow(@UriVariable("id") id: String) {
        workflowService.deleteWorkflow(id)
    }
}
