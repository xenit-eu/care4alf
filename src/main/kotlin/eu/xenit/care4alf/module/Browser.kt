package eu.xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import java.io.File
import org.springframework.extensions.webscripts.WebScriptRequest
import org.slf4j.LoggerFactory
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam
import org.alfresco.service.cmr.repository.NodeRef
import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.service.cmr.model.FileFolderService
import eu.xenit.care4alf.json
import org.alfresco.service.cmr.repository.NodeService
import org.alfresco.model.ContentModel
import org.alfresco.service.cmr.repository.StoreRef
import org.json.JSONObject
import java.io.Serializable
import org.alfresco.service.namespace.QName
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter
import org.alfresco.service.cmr.dictionary.DictionaryService
import org.alfresco.service.namespace.NamespaceService
import eu.xenit.care4alf.JsonObject
import org.alfresco.service.cmr.dictionary.PropertyDefinition
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable
import org.alfresco.service.cmr.search.SearchService
import eu.xenit.care4alf.JsonArray
import eu.xenit.care4alf.JsonRoot
import org.alfresco.repo.policy.BehaviourFilter
import org.springframework.beans.factory.annotation.Qualifier
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import java.util.Date
import org.alfresco.util.ISO8601DateFormat
import eu.xenit.care4alf.web.RestErrorHandling
import org.alfresco.service.transaction.TransactionService
import org.alfresco.service.cmr.security.PermissionService

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/browser", families = array("care4alf"), description = "node browser")
Authentication(AuthenticationType.ADMIN)
public class Browser [Autowired](
        private val filefolderService: FileFolderService,
        Qualifier("nodeService") private val nodeService: NodeService,
        private val dictionaryService: DictionaryService,
        private val namespaceService: NamespaceService,
        private val searchService: SearchService,
        private val transactionService: TransactionService,
        private val policyBehaviourFilter: BehaviourFilter,
        private val permissionService: PermissionService
    ): RestErrorHandling {
    override var logger = LoggerFactory.getLogger(javaClass)
    private val serializer = DefaultTypeConverter.INSTANCE

    Uri(value = array("/upload"), method = HttpMethod.POST)
    fun upload(request: WebScriptRequest) {
        logger.info(request.getContent().getContent())
    }

    Uri(value = array("/rootNodes"))
    fun rootNodes() = json {
        iterable(nodeService.getAllRootNodes(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE), nodesToBasicJson())
    }

    Uri(value = array("/find"), method = HttpMethod.POST)
    fun find(request: WebScriptRequest) = json {
        val requestBody = request.getContent()?.getContent()
        iterable(searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_FTS_ALFRESCO, requestBody).getNodeRefs(), nodesToBasicJson())
    }

    Uri(array("/details"))
    fun details(RequestParam noderef: NodeRef) = json {
        val path = nodeService.getPath(noderef)
        val name = nodeService.getProperty(noderef, ContentModel.PROP_NAME)
        obj {
            entry("name", name)
            entry("qnamePath", path.toPrefixString(namespaceService))
            entry("displayPath", path.toDisplayPath(nodeService, permissionService) + "/" + name)
            entry("noderef", noderef)
            entry("type", nodeService.getType(noderef))
            key("properties") {
                obj {
                    for (pair in nodeService.getProperties(noderef)) {
                        val qname = pair.getKey()
                        val propertyValue = pair.getValue()

                        if (propertyValue != null) {
                            val propDef = dictionaryService.getProperty(qname)

                            val qnameString = if (propDef != null) {
                                qname.toPrefixString(namespaceService)
                            } else {
                                qname.toString()
                            }

                            if (propertyValue is List<*>) {
                                key(qnameString) {
                                    iterable(propertyValue) { item ->
                                        value(format(item!!))
                                    }
                                }
                            } else {
                                entry(qnameString, format(propertyValue))
                            }
                        }
                    }
                }
            }
            key("aspects") {
                iterable(nodeService.getAspects(noderef)) { aspect ->
                    value(aspect)
                }
            }
            key("children") {
                iterable(nodeService.getChildAssocs(noderef)) { childAssoc ->
                    obj {
                        entry("noderef", childAssoc.getChildRef())
                        entry("name", childAssoc.getQName().toString())
                        entry("type", nodeService.getType(childAssoc.getChildRef()))
                    }
                }
            }
        }
    }

    private fun convert(propDef: PropertyDefinition?, propertyValue: Serializable): Any? {
        val converted = serializer.convert(propDef?.getDataType(), propertyValue)
        logger.debug("converted $propertyValue<${propertyValue.javaClass}> to $converted<${converted.javaClass}> using datatype ${propDef?.getDataType()}")
        return converted
    }

    Uri(array("/{noderef}/properties/{qname}"), method = HttpMethod.PUT)
    fun saveProperty(UriVariable noderef: NodeRef, UriVariable qname: QName, body: JSONObject) {
        policyBehaviourFilter.disableBehaviour(noderef, ContentModel.ASPECT_AUDITABLE)
        try {
            nodeService.setProperty(noderef, qname, body.getString("value") as? Serializable)
        } finally {
            policyBehaviourFilter.enableBehaviour(noderef, ContentModel.ASPECT_AUDITABLE)
        }
    }

    Uri(array("/{noderef}/properties/{qname}"), method = HttpMethod.DELETE)
    fun deleteProperty(UriVariable noderef: NodeRef, UriVariable qname: QName) {
        nodeService.removeProperty(noderef, qname)
    }

    Uri(array("/aspects"))
    fun aspects() = json {
        obj {
            dictionaryService.getAllModels().forEach { model ->
                val modelAspects = dictionaryService.getAspects(model)
                if (modelAspects.notEmpty) {
                    key(model.toString()) {
                        iterable(modelAspects) { aspect ->
                            obj {
                                entry("qname", aspect)
                                entry("title", dictionaryService.getAspect(aspect).getTitle())
                            }
                        }
                    }
                }
            }
        }
    }

    Uri(value = array("/{noderef}/aspects/{aspect}"), method = HttpMethod.POST, defaultFormat = "json")
    fun addAspect(UriVariable noderef: NodeRef, UriVariable aspect: String) {
        // need a new transaction, so any on-commit handler can throw errors now and be properly intercepted
        transactionService.getRetryingTransactionHelper().doInTransaction({
            // bug in DE 1.1.3 causes direct QName binding to fail
            nodeService.addAspect(noderef, QName.createQName(aspect), null)
        }, false, true)
    }

    Uri(value = array("{noderef}/aspects/{aspect}"), method = HttpMethod.DELETE, defaultFormat = "json")
    fun removeAspect(UriVariable noderef: NodeRef, UriVariable aspect: String) {
        transactionService.getRetryingTransactionHelper().doInTransaction({
            nodeService.removeAspect(noderef, QName.createQName(aspect))
        }, false, true)
    }

    Uri(value = array("{noderef}"), method = HttpMethod.DELETE, defaultFormat = "json")
    fun deleteNode(UriVariable noderef: NodeRef) {
        nodeService.addAspect(noderef, ContentModel.ASPECT_TEMPORARY, null)
        nodeService.deleteNode(noderef)
    }

    fun nodesToBasicJson(): JsonRoot.(NodeRef) -> Unit {
        return { (node: NodeRef) ->
            obj {
                entry("name", nodeService.getProperty(node, ContentModel.PROP_NAME))
                entry("noderef", node)
                entry("type", nodeService.getType(node).toString())
            }
        }
    }

    fun format(propertyValue: Any): String {
        return DefaultTypeConverter.INSTANCE.convert(javaClass<String>(), propertyValue)
    }
}