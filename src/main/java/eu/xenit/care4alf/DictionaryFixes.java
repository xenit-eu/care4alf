package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * Created by willem on 4/6/16.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/dictionaryfixes", families = {"care4alf"}, description = "Dictionary fixes")
public class DictionaryFixes {

    @Autowired
    DataSource dataSource;

    @Autowired
    DictionaryService dictionaryService;

    @Uri("/")
    public void restGetResidualProperties(WebScriptResponse res) throws SQLException, IOException {
        res.getWriter().write(this.getResidualProperties().toString());
    }

    public List<QName> getResidualProperties() throws SQLException {
        List<QName> properties = new ArrayList<QName>();
        final Connection connection = dataSource.getConnection();
        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery(
                    "select n.uri, q.local_name " +
                    "from alf_qname as q join alf_namespace  as n on q.ns_id=n.id  " +
                    "where q.id in (select distinct(qname_id) from alf_node_properties)");
            while (rs.next()) {
                QName qname = QName.createQName(rs.getString(1), rs.getString(2));
                if(this.dictionaryService.getProperty(qname) == null)
                    properties.add(qname);
            }
            rs.close();
        }
        finally {
            connection.close();
        }
        return properties;
    }
}
