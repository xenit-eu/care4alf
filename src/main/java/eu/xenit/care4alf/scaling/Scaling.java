package eu.xenit.care4alf.scaling;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 6/16/15.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/scaling", families = {"care4alf"}, description = "Scaling Alfresco")
public class Scaling {
    @Autowired
    private DataSource dataSource;

    private Map<String,String> queries;

    public Scaling()
    {
        queries = new HashMap<String,String>();
        queries.put("N1","select count( * ) from alf_node where store_id = (select id from alf_store where protocol = 'workspace' and identifier = 'SpacesStore')");
        queries.put("N2","select count( * ) from alf_node where store_id = (select id from alf_store where protocol = 'archive' and identifier = 'SpacesStore')");
        queries.put("N3","select count( * ) from alf_node where store_id = (select id from alf_store where protocol = 'workspace' and identifier = 'version2Store')");
        queries.put("T","select count( * ) from alf_transaction");
        queries.put("A","select count( * ) from alf_access_control_list");
        queries.put("X","select count( * ) from alf_acl_change_set");
    }

    private int query(String query) throws SQLException {
        int r = -1;
        final Connection connection = dataSource.getConnection();
        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                r = rs.getInt(1);
            }
            rs.close();
        } finally {
            connection.close();
        }
        return r;
    }

    private int get(String var) throws SQLException {
        return query(this.queries.get(var));
    }

    @Uri("NTAX/{var}")
    public void getN(final @UriVariable String var, final WebScriptResponse response) throws IOException, SQLException, JSONException {
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.object();
        json.key(var);
        json.value(this.query(this.queries.get(var)));
        json.endObject();
    }

    @Uri("NTAX")
    public void getNTAX(final WebScriptResponse response) throws IOException, SQLException, JSONException {
        final JSONWriter json = new JSONWriter(response.getWriter());
        String[] vars = new String[]{"N1","N2","N3","T","A","X"};
        json.object();
        for (String var : vars) {
            json.key(var);
            json.value(this.get(var));
        }
        json.endObject();
    }
}