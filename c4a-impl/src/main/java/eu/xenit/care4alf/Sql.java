package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.StatusResolution;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 6/6/16.
 */

@WebScript(families = "care4alf", description = "SQL query")
@Authentication(AuthenticationType.ADMIN)
@Component
public class Sql {

    private final Logger logger = LoggerFactory.getLogger(Sql.class);

    @Autowired
    private DataSource dataSource;

    @Uri(value = "/xenit/care4alf/sql", method = HttpMethod.POST)
    public Resolution searchQuery(JSONObject params, WebScriptResponse res)
            throws IOException, SQLException, JSONException {
        String sqlQuery = params.getString("query");
        logger.debug("running query: {}", sqlQuery);
        try {
            final List<List<String>> results = this.query(sqlQuery);
            return new JsonWriterResolution() {
                @Override
                protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                    jsonWriter.array();
                    for (List<String> row : results) {
                        jsonWriter.array();
                        for (String el : row) {
                            jsonWriter.value(el);
                        }
                        jsonWriter.endArray();
                    }
                    jsonWriter.endArray();
                }
            };
        } catch (SQLTimeoutException sqlTimeoutException) {
            logger.error("Query {} timed out.", sqlQuery, sqlTimeoutException);
            return new StatusResolution(500, "Query execution timed out.");
        } catch (Exception e) {
            logger.error("Error in query() function:", e);
            return new StatusResolution(500, e.getMessage());
        }
    }

    public List<List<String>> query(String query) throws SQLException {
        List<List<String>> results = new ArrayList<List<String>>();
        final Connection connection = dataSource.getConnection();
        try {
            final Statement stmt = connection.createStatement();
            stmt.setQueryTimeout(600);
            final ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData rsmd = rs.getMetaData();
            int columns = rsmd.getColumnCount();
            List<String> labelrow = new ArrayList<String>();
            for (int i = 1; i <= columns; i++) {
                labelrow.add(rsmd.getColumnName(i));
            }
            results.add(labelrow);
            while (rs.next()) {//TODO: column names
                List<String> row = new ArrayList<String>();
                for (int i = 1; i <= columns; i++) {
                    row.add(rs.getString(i));
                }
                results.add(row);
            }

            logger.debug("nmbr of results: " + results.size());
            rs.close();
        } finally {
            connection.close();
        }
        return results;
    }

    @Uri("/xenit/care4alf/queries")
    public Resolution getQueries(WebScriptResponse response) {
        return new JsonWriterResolution() {

            @Override
            protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                jsonWriter.array();
                for (Query query : getTheQueries()) {
                    jsonWriter.object();
                    jsonWriter.key("name");
                    jsonWriter.value(query.getName());
                    jsonWriter.key("query");
                    jsonWriter.value(query.getQuery());
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
            }
        };
    }

    private List<Query> getTheQueries() {
        List<Query> queries = new ArrayList<>();
        queries.add(new Query("Clear", ""));
        queries.add(new Query("20 Biggest folders",
                "select distinct(parent_node_id), count(*) as c from alf_child_assoc GROUP BY parent_node_id ORDER BY c DESC LIMIT 20"));
        queries.add(new Query("Documents of over 40MB", docs_over_40_mb));
        queries.add(new Query("Get doc info from UUID", doc_from_uuid));
        queries.add(new Query("Get doc info from File System path", doc_from_path));
        queries.add(new Query("Get the number of users",
                "SELECT count(*) FROM alf_node AS n, alf_qname AS q WHERE n.type_qname_id=q.id AND q.local_name='user';"));
        queries.add(new Query("Get number of nodes with content",
                "SELECT count(*) FROM alf_node AS n, alf_qname AS q WHERE n.type_qname_id=q.id AND q.local_name='content';"));
        queries.add(new Query("Number of documents of type X",
                "SELECT count(*) FROM alf_node AS n, alf_qname AS q, alf_node_properties AS p WHERE n.type_qname_id=q.id\n "
                        +
                        "AND p.node_id=n.id AND p.qname_id IN (SELECT id FROM alf_qname WHERE local_name='name') AND q.local_name='content' AND p.string_value LIKE '%.X';"));
        queries.add(new Query("All documents in specific Store",
                "SELECT * FROM alf_node WHERE store_id=6 AND type_qname_id=51;"));
        queries.add(new Query("Orphaned nodes", "SELECT * FROM alf_content_url WHERE orphan_time IS NOT NULL;"));
        queries.add(new Query("get nodecount per year of creation.",
                "SELECT COUNT(id), jaar FROM (SELECT id AS id, EXTRACT(YEAR FROM TO_DATE(SUBSTR(audit_created,0,10), 'YYYY-MM-DD')) AS jaar FROM alf_node WHERE store_id=6 ) GROUP BY jaar\n"
                        +
                        "\n" +
                        "-- Query set up for oracle sql; for postgres, add alias to subquery."));
        return queries;
    }

    private String docs_over_40_mb =
            "SELECT n.id AS \"Node ID\", n.store_id AS \"Store ID\", round(u.content_size/1024/1024,2) AS \"Size (MB)\", n.uuid AS \"Document ID (UUID)\", "
                    +
                    "n.audit_creator AS \"Creator\", n.audit_created AS \"Creation Date\", n.audit_modifier AS \"Modifier\", n.audit_modified AS \"Modification Date\", p1.string_value AS \"Document Name\", u.content_url AS \"Location\" \n"
                    +
                    "FROM alf_node AS n, alf_node_properties AS p, alf_node_properties AS p1, alf_namespace AS ns, alf_qname AS q, alf_content_data AS d, alf_content_url AS u "
                    +
                    "WHERE n.id=p.node_id AND ns.id=q.ns_id AND p.qname_id=q.id AND p.long_value=d.id AND d.content_url_id=u.id AND p1.node_id=n.id AND p1.qname_id IN (SELECT id FROM alf_qname WHERE local_name='name') "
                    +
                    "AND round(u.content_size/1024/1024,2)>40 ORDER BY u.content_size DESC;";

    private String doc_from_uuid =
            "SELECT n.id AS \"Node ID\", n.store_id AS \"Store ID\", round(u.content_size/1024/1024,2) AS \"Size (MB)\", n.uuid AS \"Document ID (UUID)\", "
                    +
                    "n.audit_creator AS \"Creator\", n.audit_created AS \"Creation Date\", n.audit_modifier AS \"Modifier\", n.audit_modified AS \"Modification Date\", p1.string_value AS \"Document Name\", u.content_url AS \"Location\" \n"
                    +
                    "FROM alf_node AS n, alf_node_properties AS p, alf_node_properties AS p1, alf_namespace AS ns, alf_qname AS q, alf_content_data AS d, alf_content_url AS u\n "
                    +
                    "WHERE n.id=p.node_id AND ns.id=q.ns_id AND p.qname_id=q.id AND p.long_value=d.id AND d.content_url_id=u.id AND p1.node_id=n.id AND p1.qname_id IN (SELECT id FROM alf_qname WHERE local_name='name') \n "
                    +
                    "AND n.uuid='******************';";

    private String doc_from_path =
            "SELECT n.id AS \"Node ID\", n.store_id AS \"Store ID\", round(u.content_size/1024/1024,2) AS \"Size (MB)\", n.uuid AS \"Document ID (UUID)\", "
                    +
                    "n.audit_creator AS \"Creator\", n.audit_created AS \"Creation Date\", n.audit_modifier AS \"Modifier\", n.audit_modified AS \"Modification Date\", p1.string_value AS \"Document Name\", u.content_url AS \"Location\" \n"
                    +
                    "FROM alf_node AS n, alf_node_properties AS p, alf_node_properties AS p1, alf_namespace AS ns, alf_qname AS q, alf_content_data AS d, alf_content_url AS u\n "
                    +
                    "WHERE n.id=p.node_id AND ns.id=q.ns_id AND p.qname_id=q.id AND p.long_value=d.id AND d.content_url_id=u.id AND p1.node_id=n.id AND p1.qname_id IN (SELECT id FROM alf_qname WHERE local_name='name')\n "
                    +
                    "AND u.content_url='***************************';";

    private class Query {

        private String name;
        private String query;

        public Query(String name, String query) {
            this.name = name;
            this.query = query;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }
}