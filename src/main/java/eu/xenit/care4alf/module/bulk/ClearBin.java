package eu.xenit.care4alf.module.bulk;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.transaction.TransactionService;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by willem on 3/3/16.
 *
 * Created for Alfresco 4.1 to clean the recycle bin using a db query like this:
 *  For Oracle:
 *      select concat('archive://SpacesStore/',n.uuid) from alf_node_aspects a join alf_node n on a.NODE_ID=n.ID where qname_id in (select id from alf_qname where local_name = 'archived') and rownum <= 1
 *
 *  For postgresql:
 *      select concat('archive://SpacesStore/',uuid) from alf_node where id in (select node_id from alf_node_aspects where qname_id in (select id from alf_qname where local_name = 'archived') limit 1)
 *
 **
 */
@Component
@Authentication(AuthenticationType.ADMIN)
@Transaction(TransactionType.REQUIRED)
public class ClearBin {
    private final Logger logger = LoggerFactory.getLogger(ClearBin.class);

    @Autowired
    private NodeService nodeService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TransactionService transactionService;

    @Uri(value = "/xenit/care4alf/bulk/db")
    public void clearbin(@RequestParam String query,
                         @RequestParam(defaultValue = "2147483647") int n,
                         final WebScriptResponse response) throws IOException, JSONException, SQLException, SystemException {
        logger.debug("query: " + query);
        logger.debug("n: " + n);

        long startTime = System.currentTimeMillis();

        int count = 1;
        List<NodeRef> nodeRefs = Collections.emptyList();
        do{
            UserTransaction trx = this.transactionService.getNonPropagatingUserTransaction(false);
            try {
                nodeRefs = query(query);
                trx.begin();
                for (NodeRef nodeRef : nodeRefs) {
                    nodeService.deleteNode(nodeRef);
                }
                count += nodeRefs.size();
                logger.debug("Count: " + count);
                trx.commit();
            } catch (Throwable e) {
                trx.rollback();
            }
        }while(count <= n && nodeRefs.size() > 0);
        long endTime   = System.currentTimeMillis();
        long duration = endTime - startTime;
        logger.info("Duration in seconds: " + duration/1000d);
        logger.info((nodeRefs.size()/(duration/1000d)) + " docs/s");
    }

    private List<NodeRef> query(String query) throws SQLException {
        logger.debug("Querying: " + query);
        List<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        final Connection connection = dataSource.getConnection();
        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                NodeRef nodeRef = new NodeRef(rs.getString(1));
                nodeRefs.add(nodeRef);
            }
            rs.close();
        }
        finally {
            connection.close();
        }
        logger.debug("Result: " + nodeRefs.size() + " noderefs");
        return nodeRefs;
    }
}