package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.MetadataCSV;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ActionCsvWorker extends ActionWorker {
    private final Logger logger = LoggerFactory.getLogger(ActionCsvWorker.class);
    private MetadataCSV metadata;

    public ActionCsvWorker(JSONObject parameters, MetadataCSV metadata) {
        super(parameters);
        this.metadata = metadata;
    }
    @Override
    public void process(NodeRef entry) throws Throwable {
        String actionName = parameters.getString("Action-name");
        Map<String, Serializable> params = getparams();
        putValuesInParams(params, actionName);

        // Different from ActionWorker: Add a param for each column in the csv
        QName qName = QName.createQName(metadata.getPropertyName(), this.nameSpacePrefixResolver);
        String key = (String) nodeService.getProperty(entry, qName);
        putCsvColumnsInParams(params, actionName, key);

        ActionService actionService = serviceRegistry.getActionService();
        Action action = actionService.createAction(actionName, params);
        actionService.executeAction(action, entry);

    }

    protected void putCsvColumnsInParams(Map<String, Serializable> params, String actionName, String key) {
        ActionService actionService = serviceRegistry.getActionService();
        List<ParameterDefinition> paramDefs = actionService.getActionDefinition(actionName).getParameterDefinitions();
        for (int i = 0; i < metadata.getCsvHeaders().length; i++) {
            String header = metadata.getCsvHeaders()[i];
            QName paramType = null;
            for (ParameterDefinition paramDef : paramDefs) {
                if (paramDef.getName().equals(header)){
                    paramType = paramDef.getType();
                    break;
                }
            }
            if (paramType == null){
                logger.info("Column {} exists in CSV but not in action params", header);
                continue;
            }
            DataTypeDefinition dataType = serviceRegistry.getDictionaryService().getDataType(paramType);
            String value = metadata.getCSVcontents().get(key)[i];
            params.put(header, (Serializable) DefaultTypeConverter.INSTANCE.convert(dataType, value));
        }
    }
}
