package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import eu.xenit.care4alf.JsonRoot
import eu.xenit.care4alf.json
import org.alfresco.model.ContentModel
import org.alfresco.repo.admin.SysAdminParams
import org.alfresco.repo.security.authentication.AuthenticationUtil
import org.alfresco.service.cmr.repository.NodeService
import org.alfresco.service.cmr.security.PermissionService
import org.alfresco.service.cmr.workflow.WorkflowInstance
import org.alfresco.service.cmr.workflow.WorkflowService
import org.alfresco.service.cmr.workflow.WorkflowTask
import org.alfresco.service.cmr.workflow.WorkflowTaskQuery
import org.alfresco.service.namespace.NamespacePrefixResolver
import org.alfresco.service.namespace.QName
import org.alfresco.util.ISO8601DateFormat
import org.alfresco.util.UrlUtil
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.io.Serializable
import java.util.*

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/workflow/instances", families = arrayOf("care4alf"), description = "Workflow instances")
@Authentication(AuthenticationType.ADMIN)
public class WorkflowInstances @Autowired constructor(
        private val workflowService: WorkflowService,
        private val permissionService: PermissionService,
        private val sysadminParams: SysAdminParams,
        private val nodeService: NodeService,
        private val nameSpacePrefixResolver: NamespacePrefixResolver
    ) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Uri(value = "/active", defaultFormat = "json")
    fun activeInstances() = json {
        iterable(workflowService.getActiveWorkflows(), instanceToJson(false))
    }

    @Uri(value = "/find/task/{taskid}", defaultFormat = "json")
    fun instanceByTask(@UriVariable taskid: String) = json {
        val workflowTask = AuthenticationUtil.runAsSystem {
            workflowService.getTaskById(taskid)
        }
        iterable(listOf(workflowTask.getPath().getInstance()), instanceToJson(includeTasks = true))
    }

    @Uri(value = "/find/instance/{instanceid}", defaultFormat = "json")
    fun instanceById(@UriVariable instanceid: String) = json {
        iterable(listOf(workflowService.getWorkflowById(instanceid)), instanceToJson(includeTasks = true))
    }

    fun instanceToJson(includeTasks: Boolean): JsonRoot.(WorkflowInstance) -> Unit {
        return { instance ->
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
                    val tasks = AuthenticationUtil.runAsSystem {
                        getTasksForInstance(instance)
                    }
                    key("tasks") {
                        iterable(tasks, taskToJson())
                    }
                }
            }
        }
    }

    fun taskToJson(): JsonRoot.(WorkflowTask) -> Unit {
        return {
            task -> obj {
                entry("id", task.getId())
                entry("name", task.getName())
                entry("title", task.getTitle())
                entry("state", task.getState())
                entry("description", task.getDescription())
                key("properties") {
                    obj {
                        for (property in task.getProperties()) {
                            entry(property.key.toString(), property.value)
                        }
                    }
                }
            }
        }

    }

    @Uri(value = "/{workflowid}/tasks", defaultFormat = "json")
    fun tasksForInstance(@UriVariable workflowid: String) = json {
        val tasks = AuthenticationUtil.runAsSystem {
            getTasksForInstance(workflowService.getWorkflowById(workflowid))
        }
        iterable(tasks, taskToJson())
    }

    fun getTasksForInstance(instance: WorkflowInstance): List<WorkflowTask>? {
        val query = WorkflowTaskQuery()
        query.setProcessId(instance.getId())
        query.setTaskState(null)
        query.setActive(null)
        return workflowService.queryTasks(query)
    }

    @Uri(value = "/{id}/cancel", method = HttpMethod.DELETE)
    fun cancelWorkflow(@UriVariable("id") id: String) {
        workflowService.cancelWorkflow(id)
    }

    @Uri(value = "/{id}/delete", method = HttpMethod.DELETE)
    fun deleteWorkflow(@UriVariable("id") id: String) {
        workflowService.deleteWorkflow(id)
    }

    @Uri(value = "/active", method = HttpMethod.DELETE)
    fun deleteAllActive() {
        for (wf in workflowService.getActiveWorkflows()) {
            try {
                workflowService.deleteWorkflow(wf.getId())
                logger.debug("Deleted workflow instance ${wf.getId()}.")
            }
            catch(ex: Exception) {
                logger.info("Failed to delete workflow instance ${wf.getId()}: ${ex.message}.")
            }
        }
    }

    @Uri(value = "/tasks/{id}/release", method = HttpMethod.POST, defaultFormat = "json")
    fun releaseTask(@UriVariable("id") id: String) = json {
        logger.error("Id is {}", id);
        val props: MutableMap<QName, Serializable?> = hashMapOf(ContentModel.PROP_OWNER to null);
        val task = workflowService.updateTask(id, props, null, null);
        obj {
            entry("id", task.getId())
            entry("description", task.getDescription())
            key("properties") {
                obj {
                    for (property in task.getProperties()) {
                        entry(property.key.toString(), property.value)
                    }
                }
            }
        }
    }

    @Uri(value = "/tasks/{id}/setProperty", method = HttpMethod.POST, defaultFormat = "json")
    fun setTaskProperty(@UriVariable("id") id: String, payload: JSONObject) = json {
        logger.info("Setting property on {}", id);
        val type = payload.getString("type");
        val qname : QName = QName.createQName(payload.getString("qname"), nameSpacePrefixResolver);
        var props: MutableMap<QName, Serializable?> = hashMapOf();
        when (type) {
            "Date" -> {
                val date: Date = ISO8601DateFormat.parse(payload.getString("value"));
                props = hashMapOf(qname to date);
            }
            "Integer" -> {
                val integer: Int = Integer.parseInt(payload.getString("value"));
                props = hashMapOf(qname to integer);
            }
            "String" -> props = hashMapOf(qname to payload.getString("value"));
        }
        if (props.isEmpty()) {
            throw IllegalArgumentException("Type $type not recognized, should be String, Integer or Date");
        }
        val task = workflowService.updateTask(id, props, null, null);
        obj {
            entry("id", task.getId())
            entry("description", task.getDescription())
            key("properties") {
                obj {
                    for (property in task.getProperties()) {
                        entry(property.key.toString(), property.value)
                    }
                }
            }
        }
    }
}