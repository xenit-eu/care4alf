package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.alfresco.repo.domain.propval.PropertyValueDAO;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.util.Pair;
import org.json.JSONException;
import org.json.JSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Serializable;
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

    @Uri("/")
    public void listAttributes(WebScriptResponse res) throws IOException, SQLException, JSONException {
        final JSONWriter json = new JSONWriter(res.getWriter());
        json.array();
        for(Attribute attribute : this.list()){
            json.array();
                json.value(attribute.getKey1());
                json.value(attribute.getKey2());
                json.value(attribute.getKey3());
                json.value(attribute.getAtt());
            json.endArray();
        }
        json.endArray();
    }

    @Autowired
    PropertyValueDAO propertyValueDAO;
    @Autowired
    private DataSource dataSource;
    public List<Attribute> list() throws SQLException {
        List<Attribute> attributes = new ArrayList<Attribute>();
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
                attributes.add(new Attribute(
                                pair1.getSecond() == null?null:pair1.getSecond().toString(),
                                pair2.getSecond() == null?null:pair2.getSecond().toString(),
                                pair3.getSecond() == null?null:pair3.getSecond().toString(),
                                this.attributeService.getAttribute(pair1.getSecond(), pair2.getSecond(), pair3.getSecond())
                        )
                );
            }
            rs.close();
        } finally {
            connection.close();
        }
        return attributes;
    }

    @Uri(value = "/", method = HttpMethod.POST)
    public void addAttribute(WebScriptResponse res, @RequestParam(delimiter = ";") String[] keys, @RequestParam String value) throws IOException, SQLException, JSONException {
        this.attributeService.createAttribute(value, keys);
    }

    @Uri(value = "/", method = HttpMethod.DELETE)
    public void removeAttribute(WebScriptResponse res, @RequestParam(delimiter = ";") String[] keys) throws IOException, SQLException, JSONException {
        this.attributeService.removeAttribute(keys);
    }

    class Attribute{
        private String key1, key2, key3;
        private Serializable att;

        public Attribute(String value1, String value2, String key3, Serializable att) {
            this.key1 = value1;
            this.key2 = value2;
            this.key3 = key3;
            this.att = att;
        }

        public String getKey1() {
            return key1;
        }

        public String getKey2() {
            return key2;
        }

        public String getKey3() {
            return key3;
        }

        public Serializable getAtt() {
            return att;
        }

        public String toString(){
            return String.format("(%s,%s,%s) '%s'", this.getKey1(), this.getKey2(), this.getKey3(), this.getAtt().toString());
        }
    }

}