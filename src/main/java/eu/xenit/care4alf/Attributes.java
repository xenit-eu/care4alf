package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import org.alfresco.repo.domain.propval.PropertyValueDAO;
import org.alfresco.repo.usage.RepoUsageComponent;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by willem on 4/18/16.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/attributes", families = {"care4alf"}, description = "Attributes")
@Authentication(AuthenticationType.ADMIN)
public class Attributes {

    @Autowired
    AttributeService attributeService;

    @Autowired
    RepoUsageComponent repoUsageComponent;

    @Uri("repoUsages")
    public void getRepoUsages(WebScriptResponse res) throws IOException {
        Long lastUpdateUsers = (Long) attributeService.getAttribute(
                ".repoUsages", "current", "lastUpdateUsers");
        Long users = (Long) attributeService.getAttribute(
                ".repoUsages", "current", "users");
        Long lastUpdateDocuments = (Long) attributeService.getAttribute(
                ".repoUsages", "current", "lastUpdateDocuments");
        Long documents = (Long) attributeService.getAttribute(
                ".repoUsages", "current", "documents");
        Writer writer = res.getWriter();
        writer.write(lastUpdateUsers.toString());
        writer.write("\n");
        writer.write(users.toString());
        writer.write("\n");
        writer.write(lastUpdateDocuments.toString());
        writer.write("\n");
        writer.write(documents.toString());
    }

    @Uri("usage")
    public void getRepoUsage(WebScriptResponse response) throws IOException {
        response.getWriter().write(this.repoUsageComponent.getUsage().toString());
    }

    @Uri("/")
    public void listAttributes(WebScriptResponse res) throws IOException, SQLException {
        res.getWriter().write(this.list().toString());
    }

    @Autowired
    PropertyValueDAO propertyValueDAO;
    @Autowired
    private DataSource dataSource;
    public List<Pair<String, Serializable>> list() throws SQLException {
        List<Pair<String, Serializable>> attributes = new ArrayList<Pair<String, Serializable>>();
        String query = "select * from alf_prop_unique_ctx";
        final Connection connection = dataSource.getConnection();
        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Long key1 = rs.getLong(3);
                Long key2 = rs.getLong(4);
                Long key3 = rs.getLong(5);
                Pair<Long,Serializable> pair1 = propertyValueDAO.getPropertyValueById(key1);
                Pair<Long,Serializable> pair2 = propertyValueDAO.getPropertyValueById(key2);
                Pair<Long,Serializable> pair3 = propertyValueDAO.getPropertyValueById(key3);
                attributes.add(new Pair<String, Serializable>(
                        String.format("%s %s %s", pair1.getSecond(), pair2.getSecond(), pair3.getSecond()),
                        this.attributeService.getAttribute(pair1.getSecond(), pair2.getSecond(), pair3.getSecond())));

            }
            rs.close();
        } finally {
            connection.close();
        }
        return attributes;
    }

}