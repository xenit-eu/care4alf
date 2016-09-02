package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import org.json.JSONException;
import org.json.JSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by willem on 6/6/16.
 */
@WebScript(families = "care4alf", description = "SQL query")
@Authentication(AuthenticationType.ADMIN)
@Component
public class Sql {

    @Autowired
    private DataSource dataSource;

    @Uri("/xenit/care4alf/sql")
    public Resolution searchQuery(@RequestParam String query,  WebScriptResponse res) throws IOException, SQLException {
        final List<List<String>> results = this.query(query);
        return new JsonWriterResolution() {
            @Override
            protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                jsonWriter.array();
                for(List<String> row : results){
                    jsonWriter.array();
                    for(String el : row){
                        jsonWriter.value(el);
                    }
                    jsonWriter.endArray();
                }
                jsonWriter.endArray();
            }
        };
    }

    public List<List<String>> query(String query) throws SQLException {
        List<List<String>> results = new ArrayList<List<String>>();
        final Connection connection = dataSource.getConnection();
        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData rsmd = rs.getMetaData();
            int columns = rsmd.getColumnCount();
            while (rs.next()) {//TODO: column names
                List<String> row = new ArrayList<String>();
                for(int i = 1; i <= columns; i++)
                    row.add(rs.getString(i));
                results.add(row);
            }
            rs.close();
        } finally {
            connection.close();
        }
        return results;
    }
}