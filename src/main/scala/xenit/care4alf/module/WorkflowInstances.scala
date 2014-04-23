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
import org.alfresco.repo.admin.SysAdminParams
import org.alfresco.util.UrlUtil
import org.springframework.util.StringUtils

import org.alfresco.repo.security.authentication.AuthenticationUtil.runAsSystem
import xenit.care4alf.alfresco.Implicits._

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/workflow/instances", families = Array("care4alf"), description = "Workflow instances")
@Authentication(AuthenticationType.ADMIN)
class WorkflowInstances extends Json with Logging with HasNodeService with HasNamespaceService with RestErrorHandling {
    @Autowired var workflowService: WorkflowService = null
    @Autowired var sysadminParams: SysAdminParams = null

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
        val workflowTask = runAsSystem {
            workflowService.getTaskById(taskid)
        }
        json.array()
        instanceToJson(workflowTask.getPath.getInstance(), jsonHelper, includeTasks = true)
        json.endArray()
    }

    @Uri(value = Array("/find/instance/{instanceid}"), defaultFormat = "json")
    def instanceById(@Attribute jsonHelper: JsonHelper, @UriVariable instanceid: String) {
        val instance = workflowService.getWorkflowById(instanceid)
        if (instance != null) {
            val json = jsonHelper.json
            json.array()
            instanceToJson(instance, jsonHelper, includeTasks = true)
            json.endArray()
        } else {
            throw new IllegalArgumentException(s"No workflow instance $instanceid found.")
        }
    }


    def instanceToJson(instance: WorkflowInstance, jsonHelper: JsonHelper, includeTasks: Boolean) {
        val json = jsonHelper.json
        val initiator = instance.getInitiator
        val initiatorUsername = if (nodeService.exists(initiator)) initiator(ContentModel.PROP_USERNAME).asInstanceOf[String] else "-"
        val description = if (StringUtils.hasText(instance.getDescription)) instance.getDescription else instance.getDefinition.getDescription
        json.`object`()
            .key("description").value(description)
            .key("id").value(instance.getId)
            .key("initiator").value(initiatorUsername)
            .key("start").value(instance.getStartDate.getTime)
            .key("files").array()
        for (assoc <- nodeService.getChildAssocs(instance.getWorkflowPackage)) {
            json.`object`()
                .key("path").value(assoc.getChildRef.path)
                .key("url").value(UrlUtil.getShareUrl(sysadminParams) + "/page/document-details?nodeRef=" + assoc.getChildRef)
            .endObject()
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
        val tasks = runAsSystem {
            getTasksForInstance(workflowService.getWorkflowById(workflowid))
        }
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
        query.setProcessId(instance.getId)
        query.setTaskState(null)
        query.setActive(null)
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

    @Uri(value = Array("/active"), method = HttpMethod.DELETE)
    def deleteAllActive() {
        for (wf <- workflowService.getActiveWorkflows) {
            try {
                workflowService.deleteWorkflow(wf.getId)
                logger.debug(s"Deleted workflow instance ${wf.getId}.")
            }
            catch {
                case ex: Exception => {
                    logger.info(s"Failed to delete workflow instance ${wf.getId}: ${ex.getMessage}.")
                }
            }
        }
    }
}
