package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import eu.xenit.care4alf.JsonRoot
import eu.xenit.care4alf.helpers.contentdata.ContentDataHelper
import eu.xenit.care4alf.json
import eu.xenit.care4alf.web.RestErrorHandling
import org.alfresco.model.ContentModel
import org.alfresco.repo.domain.permissions.AclDAO
import org.alfresco.repo.i18n.StaticMessageLookup
import org.alfresco.repo.policy.BehaviourFilter
import org.alfresco.service.cmr.dictionary.DictionaryService
import org.alfresco.service.cmr.dictionary.PropertyDefinition
import org.alfresco.service.cmr.model.FileFolderService
import org.alfresco.service.cmr.repository.ContentData
import org.alfresco.service.cmr.repository.NodeRef
import org.alfresco.service.cmr.repository.NodeService
import org.alfresco.service.cmr.repository.StoreRef
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter
import org.alfresco.service.cmr.search.QueryConsistency
import org.alfresco.service.cmr.search.SearchParameters
import org.alfresco.service.cmr.search.SearchService
import org.alfresco.service.cmr.security.PermissionService
import org.alfresco.service.namespace.NamespaceService
import org.alfresco.service.namespace.QName
import org.alfresco.service.namespace.RegexQNamePattern
import org.alfresco.service.transaction.TransactionService
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.extensions.webscripts.WebScriptRequest
import org.springframework.extensions.webscripts.WebScriptResponse
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.*

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/browser", families = arrayOf("care4alf"), description = "node browser", defaultFormat = "json")
@Authentication(AuthenticationType.ADMIN)
public class Browser @Autowired constructor(
        private val filefolderService: FileFolderService,
        @Qualifier("nodeService") private val nodeService: NodeService,
        private val dictionaryService: DictionaryService,
        private val namespaceService: NamespaceService,
        private val searchService: SearchService,
        private val transactionService: TransactionService,
        private val policyBehaviourFilter: BehaviourFilter,
        private val permissionService: PermissionService,
        private val aclDAO: AclDAO,
        private val contentDataHelper: ContentDataHelper
) : RestErrorHandling {
    override var logger: Logger = LoggerFactory.getLogger(javaClass)
    private val serializer = DefaultTypeConverter.INSTANCE

    @Uri("/upload", method = HttpMethod.POST)
    fun upload(request: WebScriptRequest) {
        logger.info(request.getContent().getContent())
    }

    @Uri("/rootNodes")
    fun rootNodes() = json {
        iterable(nodeService.getAllRootNodes(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE), nodesToBasicJson())
    }

    @Uri("/find", method = HttpMethod.POST)
    fun find(j: JSONObject) = json {
        val query: String = j.getString("query")
        logger.debug("Request body: {}", query)
        if (query.matches("-?\\d+(\\.\\d+)?".toRegex())) {
            val dbid = query.toLong()
            val nodeRef = nodeService.getNodeRef(dbid)
            obj {
                key("nodes") {
                    iterable(listOf(nodeRef), nodesToBasicJson())
                }
            }
        } else if (query.toLowerCase().matches("^(workspace|archive|system|user)://.*".toRegex())) {
            logger.debug("Noderef requested directly")
            val nodeRefs = NodeRef.getNodeRefs(query)
            logger.debug(nodeRefs[0].toString())
            obj {
                key("nodes") {
                    iterable(nodeRefs, nodesToBasicJson())
                }
            }
        } else {
            //Adding sorting to guarantee order. This is required because paging does not work with
            //cursors or cache, therefore any request with more results than the max returned items
            //will result in a new query. This implies that the result of the second query may include
            //results that were already returned by the first query. By ordering both queries, the
            //skipcount will guarantee 'new' results for each.
            //The property node-dbid has been chosen since it is semantically meaningless, but is
            //present on every node in the system.
            //FTS does not include sorting in the query language. Therefore there is no other source for
            //determining sorting. This means we can hardcode sorting on node-dbid.
            //this should only change if the browser were to start offering sorting options in the ui
            val sp = SearchParameters()
            sp.addStore(StoreRef(j.optString("storeref", "workspace://SpacesStore")))
            sp.language = SearchService.LANGUAGE_FTS_ALFRESCO
            sp.query = query
            sp.addSort("sys:node-dbid", false)
            sp.queryConsistency = if (j.optString("consistency") == "transactional") {
                QueryConsistency.TRANSACTIONAL
            } else {
                QueryConsistency.EVENTUAL // Default, for counting total
            }
            sp.addLocale(Locale("*"))
            val rs = searchService.query(sp)

            obj {
                key("nodes") {
                    iterable(rs.nodeRefs, nodesToBasicJson())
                }
                entry("total", rs.numberFound)
            }
        }
    }

    @Uri("/details")
    fun details(@RequestParam noderef: NodeRef) = json {
        val path = nodeService.getPath(noderef)
        val name = nodeService.getProperty(noderef, ContentModel.PROP_NAME)
        val dbid = nodeService.getProperty(noderef, ContentModel.PROP_NODE_DBID)
        obj {
            entry("name", name)
            entry("qnamePath", path.toPrefixString(namespaceService))
            entry("displayPath", path.toDisplayPath(nodeService, permissionService) + "/" + name)
            entry("noderef", noderef)
            entry("dbid", dbid)
            entry("type", nodeService.getType(noderef))
            val immutableFields = ArrayList<String>()
            val hyperlinkedFields = ArrayList<String>()
            key("properties") {
                obj {
                    for (pair in nodeService.getProperties(noderef)) {
                        val qname = pair.key
                        val propertyValue = pair.value

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
                            } else if (propertyValue is ContentData) {
                                if (ContentData.hasContent(propertyValue)) {
                                    entry(qnameString, format(propertyValue))
                                    hyperlinkedFields.add(qnameString)
                                    immutableFields.add(qnameString);
                                    val contentDataComponents = contentDataHelper.getContentDataComponents(qnameString, propertyValue)
                                    for (component in contentDataComponents.entries) {
                                        immutableFields.add(component.value.qnamestring)
                                        entry(component.value.qnamestring, component.value.value)
                                    }
                                }
                            } else {
                                entry(qnameString, format(propertyValue))
                            }
                        }
                    }
                }
            }
            entry("immutableFields", immutableFields)
            entry("hyperlinkedFields", hyperlinkedFields)
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
            key("targetAssocs") {
                iterable(nodeService.getTargetAssocs(noderef, RegexQNamePattern.MATCH_ALL)) { assoc ->
                    obj {
                        entry("id", assoc.getId())
                        entry("sourceRef", assoc.getSourceRef())
                        entry("targetRef", assoc.getTargetRef())
                        entry("type", assoc.getTypeQName())
                    }
                }
            }
            key("sourceAssocs") {
                iterable(nodeService.getSourceAssocs(noderef, RegexQNamePattern.MATCH_ALL)) { assoc ->
                    obj {
                        entry("id", assoc.getId())
                        entry("sourceRef", assoc.getSourceRef())
                        entry("targetRef", assoc.getTargetRef())
                        entry("type", assoc.getTypeQName())
                    }
                }
            }
            key("acl") {
                val aclId = nodeService.getNodeAclId(noderef)
                val acl = aclDAO.getAcl(aclId)
                val accessControlList = aclDAO.getAccessControlList(aclId)
                obj {
                    entry("id", acl.getId())
                    entry("changeset", acl.getAclChangeSetId())
                    entry("type", acl.getAclType())
                    entry("version", acl.getAclVersion())
                    entry("inheritedacl", acl.getInheritedAcl())
                    entry("inheritsfrom", acl.getInheritsFrom())
                    entry("requiresversion", acl.getRequiresVersion())
                    key("accesscontrollist") {
                        iterable(accessControlList.getEntries()) { aclEntry ->
                            obj {
                                entry("status", aclEntry.getAccessStatus())
                                entry("acetype", aclEntry.getAceType())
                                entry("authority", aclEntry.getAuthority())
                                entry("permission", aclEntry.getPermission().getName())
                            }
                        }
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

    @Uri("/{protocol}/{identifier}/{id}/properties/{qname}", method = HttpMethod.PUT)
    fun saveProperty(@UriVariable protocol: String, @UriVariable identifier: String, @UriVariable id: String, @UriVariable qname: QName, body: JSONObject) {
        val noderef: NodeRef = NodeRef(protocol, identifier, id)
        policyBehaviourFilter.disableBehaviour(noderef, ContentModel.ASPECT_AUDITABLE)
        val originalValue: Serializable? = nodeService.getProperty(noderef, qname)
        try {
            if (body.has("multi") && body.getBoolean("multi")) {
                nodeService.setProperty(noderef, qname, body.getString("value").split(",") as? ArrayList<*>)
            } else {
                nodeService.setProperty(noderef, qname, body.getString("value") as? Serializable)
            }
        } catch (e: Exception) {
            // Something went wrong trying to set this property *and* we disabled the policy that protects integrity
            // Try to undo the damage...
            logger.warn("Failed to set property {} to new value, reverting back to its original value...")
            transactionService.retryingTransactionHelper.doInTransaction {
                nodeService.setProperty(noderef, qname, originalValue)
            }
            // Throw it again because it needs to make it to userspace
            throw e
        } finally {
            policyBehaviourFilter.enableBehaviour(noderef, ContentModel.ASPECT_AUDITABLE)
        }
    }

    @Uri("/{protocol}/{identifier}/{id}/properties/{qname}", method = HttpMethod.DELETE)
    fun deleteProperty(@UriVariable protocol: String, @UriVariable identifier: String, @UriVariable id: String, @UriVariable qname: QName) {
        val noderef: NodeRef = NodeRef(protocol, identifier, id)
        nodeService.removeProperty(noderef, qname)
    }

    @Uri("/aspects")
    fun aspects() = json {
        obj {
            dictionaryService.getAllModels().forEach { model ->
                val modelAspects = dictionaryService.getAspects(model)
                if (modelAspects.isNotEmpty()) {
                    key(model.toString()) {
                        iterable(modelAspects) { aspect ->
                            obj {
                                entry("qname", aspect)
                                entry("title", dictionaryService.getAspect(aspect).getTitle(StaticMessageLookup()))
                            }
                        }
                    }
                }
            }
        }
    }

    @Uri("types")
    fun types() = json {
        obj {
            dictionaryService.getAllModels().forEach { model ->
                val modelTypes = dictionaryService.getTypes(model)
                if (modelTypes.isNotEmpty()) {
                    key(model.toString()) {
                        iterable(modelTypes) { type ->
                            obj {
                                entry("qname", type)
                                entry("title", dictionaryService.getType(type).getTitle(StaticMessageLookup()))
                            }
                        }
                    }
                }
            }
        }
    }

    @Uri("/{protocol}/{identifier}/{id}/aspects", method = HttpMethod.POST)
    fun addAspect(@UriVariable protocol: String, @UriVariable identifier: String, @UriVariable id: String, jsonBody: JSONObject) {
        // need a new transaction, so any on-commit handler can throw errors now and be properly intercepted
        transactionService.getRetryingTransactionHelper().doInTransaction({
            // bug in DE 1.1.3 causes direct QName binding to fail
            val noderef: NodeRef = NodeRef(protocol, identifier, id)
            nodeService.addAspect(noderef, QName.createQName(jsonBody.getString("aspect")), null)
        }, false, true)
    }

    @Uri("/{protocol}/{identifier}/{id}/aspects/{aspect}", method = HttpMethod.DELETE)
    fun removeAspect(@UriVariable protocol: String, @UriVariable identifier: String, @UriVariable id: String, @UriVariable aspect: String) {
        transactionService.getRetryingTransactionHelper().doInTransaction({
            val noderef: NodeRef = NodeRef(protocol, identifier, id)
            nodeService.removeAspect(noderef, QName.createQName(aspect))
        }, false, true)
    }

    @Uri("/{protocol}/{identifier}/{id}/type", method = HttpMethod.PUT)
    fun setType(@UriVariable protocol: String, @UriVariable identifier: String, @UriVariable id: String, jsonBody: JSONObject) {
        val noderef: NodeRef = NodeRef(protocol, identifier, id)
        nodeService.setType(noderef, QName.createQName(jsonBody.getString("type")))
    }

    @Uri("/{protocol}/{identifier}/{id}", method = HttpMethod.DELETE)
    fun deleteNode(@UriVariable protocol: String, @UriVariable identifier: String, @UriVariable id: String) {
        val noderef: NodeRef = NodeRef(protocol, identifier, id)
        nodeService.addAspect(noderef, ContentModel.ASPECT_TEMPORARY, null)
        nodeService.deleteNode(noderef)
    }

    @Uri("assoc/{id}", method = HttpMethod.DELETE)
    fun deleteAssoc(@UriVariable id: Long) {
        val associationRef = nodeService.getAssoc(id)
        nodeService.removeAssociation(associationRef.getSourceRef(), associationRef.getTargetRef(), associationRef.getTypeQName())
    }

    @Uri("/deletechild", method = HttpMethod.POST)
    fun deleteChild(json: JSONObject) {
        val parentRef = NodeRef(json.getString("parent"));
        val childRef = NodeRef(json.getString("child"));
        nodeService.removeChild(parentRef, childRef);
    }

    @Uri("/child", method = HttpMethod.POST)
    public fun addChild(json: JSONObject, response: WebScriptResponse) {
        val parentRef = NodeRef(json.getString("parent"))
        val childRef = NodeRef(json.getString("child"))

        val name = nodeService.getProperty(childRef, ContentModel.PROP_NAME) as String
        val assocQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(name))

        val associationRef = nodeService.addChild(parentRef, childRef, ContentModel.ASSOC_CONTAINS, assocQName)
        response.getWriter().write(associationRef.toString())
    }

    fun nodesToBasicJson(): JsonRoot.(NodeRef) -> Unit {
        return { node: NodeRef ->
            obj {
                entry("name", nodeService.getProperty(node, ContentModel.PROP_NAME))
                entry("noderef", node)
                entry("type", nodeService.getType(node).toString())
            }
        }
    }

    fun format(propertyValue: Any): String {
        return DefaultTypeConverter.INSTANCE.convert(String::class.java, propertyValue)
    }
}