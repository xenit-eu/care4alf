package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.QName;
import org.json.JSONException;
import org.json.JSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by willem on 4/6/16.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/properties", description = "Properties")
public class Properties {

    @Autowired
    DataSource dataSource;

    @Autowired
    DictionaryService dictionaryService;

    @Uri("/residual")
    public void restGetResidualProperties(WebScriptResponse response) throws SQLException, IOException, JSONException {
        response.setContentType("application/json");
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.array();
        for(QName prop : this.getResidualProperties()){
            json.value(prop);
        }
        json.endArray();
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

    public List<QName> getResidualProperties(String filter) throws SQLException {
        List<QName> filtered = new ArrayList<QName>();
        for(QName prop : this.getResidualProperties())
        {
            if(!prop.toString().contains(filter))
                filtered.add(prop);
        }
        return filtered;
    }

    @Uri("/")
    public void list(WebScriptResponse response) throws SQLException, IOException, JSONException {
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.array();
        for(Object prop : this.list()){
            json.value(prop);
        }
        json.endArray();
    }

    public List<QNameInfo> list() throws SQLException {
        List<QNameInfo> list = new ArrayList<QNameInfo>();
        final Connection connection = dataSource.getConnection();
        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery(
                "select q.id, n.uri, q.local_name " +
                "from alf_qname as q join alf_namespace  as n on q.ns_id=n.id  " +
                "where q.id in (select distinct(qname_id) from alf_node_properties)");
            while (rs.next()) {
                list.add(new QNameInfo(rs.getLong(1), QName.createQName(rs.getString(2), rs.getString(3))));
            }
            rs.close();
        } finally {
            connection.close();
        }
        return list;
    }

    public List<QNameInfo> getQNames() throws SQLException {
        List<QNameInfo> list = new ArrayList<>();
        final Connection connection = dataSource.getConnection();
        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery(
                    "select q.id, n.uri, q.local_name from alf_qname as q join alf_namespace as n on q.ns_id=n.id");
            while (rs.next()) {
                list.add(new QNameInfo(rs.getLong(1), QName.createQName(rs.getString(2), rs.getString(3))));
            }
            rs.close();
        } finally {
            connection.close();
        }
        return list;
    }

    public class QNameInfo {
        private Long id;
        private QName qname;

        public Long getId() {
            return id;
        }

        public QName getQname() {
            return qname;
        }

        public QNameInfo(Long id, QName qname) {
            this.id = id;
            this.qname = qname;
        }

        @Override
        public String toString() {
            return "QNameInfo{" +
                    "id=" + id +
                    ", qname=" + qname +
                    '}';
        }
    }
}
