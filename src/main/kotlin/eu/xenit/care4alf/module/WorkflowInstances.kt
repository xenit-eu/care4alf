package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import org.springframework.stereotype.Component
import org.alfresco.service.cmr.workflow.WorkflowService
import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.repo.admin.SysAdminParams
import eu.xenit.care4alf.json
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import org.alfresco.service.cmr.workflow.WorkflowInstance
import org.springframework.util.StringUtils
import org.alfresco.model.ContentModel
import org.alfresco.service.cmr.repository.NodeService
import org.alfresco.service.cmr.security.PermissionService
import org.alfresco.util.UrlUtil
import org.alfresco.repo.security.authentication.AuthenticationUtil
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable
import org.alfresco.service.cmr.workflow.WorkflowTaskQuery
import org.alfresco.service.cmr.workflow.WorkflowTask
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod
import org.slf4j.LoggerFactory
import eu.xenit.care4alf.JsonRoot

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/workflow/instances", families = array("care4alf"), description = "Workflow instances")
Authentication(AuthenticationType.ADMIN)
public class WorkflowInstances [Autowired](
        private val workflowService: WorkflowService,
        private val permissionService: PermissionService,
        private val sysadminParams: SysAdminParams,
        private val nodeService: NodeService
    ) {

    private val logger = LoggerFactory.getLogger(javaClass)

    Uri(value = array("/active"), defaultFormat = "json")
    fun activeInstances() = json {
        iterable(workflowService.getActiveWorkflows(), instanceToJson(false))
    }

    Uri(value = array("/find/task/{taskid}"), defaultFormat = "json")
    fun instanceByTask(UriVariable taskid: String) = json {
        val workflowTask = AuthenticationUtil.runAsSystem {
            workflowService.getTaskById(taskid)
        }
        iterable(listOf(workflowTask.getPath().getInstance()), instanceToJson(includeTasks = true))
    }

    Uri(value = array("/find/instance/{instanceid}"), defaultFormat = "json")
    fun instanceById(UriVariable instanceid: String) = json {
        iterable(listOf(workflowService.getWorkflowById(instanceid)), instanceToJson(includeTasks = true))
    }

    fun instanceToJson(includeTasks: Boolean): JsonRoot.(WorkflowInstance) -> Unit {
        return { (instance) ->
            val initiator = instance.getInitiator()
            val initiatorUsername = if (nodeService.exists(initiator)) {
                nodeService.getProperty(initiator, ContentModel.PROP_USERNAME) as String
            } else {
                "-"
            }
            val description = if (StringUtils.hasText(instance.getDescription())) {
                instance.getDescription()
            } else {
                instance.getDefinition().getDescription()
            }

            obj {
                entry("id", instance.getId())
                entry("description", description)
                entry("initiator", initiatorUsername)
                entry("start", instance.getStartDate()?.getTime())
                key("definition") {
                    obj {
                        entry("id", instance.getDefinition().getId())
                        entry("name", instance.getDefinition().getName())
                    }
                }
                key("files") {
                    val children = nodeService.getChildAssocs(instance.getWorkflowPackage()).map({it.getChildRef()})
                    iterable(children) { file ->
                        obj {
                            entry("path", nodeService.getPath(file).toDisplayPath(nodeService, permissionService))
                            entry("url", UrlUtil.getShareUrl(sysadminParams) + "/page/document-details?nodeRef=" + file)
                        }
                    }
                }
                if (includeTasks) {
                    key("tasks") {
                        tasksForInstance(instance.getId())
                    }
                }
            }
        }
    }

    Uri(value = array("/{workflowid}/tasks"), defaultFormat = "json")
    fun tasksForInstance(UriVariable workflowid: String) = json {
        val tasks = AuthenticationUtil.runAsSystem {
            getTasksForInstance(workflowService.getWorkflowById(workflowid))
        }
        iterable(tasks) { task ->
            obj {
                entry("id", task.getId())
                entry("description", task.getDescription())
                key("properties") {
                    obj {
                        for (property in task.getProperties()) {
                            entry(property.getKey().toString(), property.getValue())
                        }
                    }
                }
            }
        }
    }

    fun getTasksForInstance(instance: WorkflowInstance): List<WorkflowTask>? {
        val query = WorkflowTaskQuery()
        query.setProcessId(instance.getId())
        query.setTaskState(null)
        query.setActive(null)
        return workflowService.queryTasks(query)
    }

    Uri(value = array("/{id}/cancel"), method = HttpMethod.DELETE)
    fun cancelWorkflow(UriVariable("id") id: String) {
        workflowService.cancelWorkflow(id)
    }

    Uri(value = array("/{id}/delete"), method = HttpMethod.DELETE)
    fun deleteWorkflow(UriVariable("id") id: String) {
        workflowService.deleteWorkflow(id)
    }

    Uri(value = array("/active"), method = HttpMethod.DELETE)
    fun deleteAllActive() {
        for (wf in workflowService.getActiveWorkflows()) {
        try {
            workflowService.deleteWorkflow(wf.getId())
            logger.debug("Deleted workflow instance ${wf.getId()}.")
        }
        catch(ex: Exception) {
            logger.info("Failed to delete workflow instance ${wf.getId()}: ${ex.getMessage()}.")
        }
    }
}
}