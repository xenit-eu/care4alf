package eu.xenit.care4alf.categoryLoader;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.servlet.FormData;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Thomas.Straetmans on 25/11/2016.
 */

@Component
@WebScript(families = "care4alf", description = "Monitoring")
@Authentication(AuthenticationType.ADMIN)
public class Category {

    private final static Logger logger = LoggerFactory.getLogger(Category.class);

    @Autowired
    ClassificationInstaller classificationInstaller;

    @Uri(value = "/xenit/care4alf/category", method = HttpMethod.POST)
    public void loadClassification(final WebScriptRequest multiPart, final WebScriptResponse response) throws IOException, ParseException {

        String name = "", nameSpace = "", json = "";

        FormData formData = (FormData) multiPart.parseContent();
        FormData.FormField[] fields = formData.getFields();
        for(FormData.FormField field : fields){
            if(field.getName().equals("name")){
                name = field.getValue();
            } else if(field.getName().equals("file")){
                logger.debug("is file? : {}", field.getIsFile());
                InputStream inputStream = field.getInputStream();
                JSONParser jsonParser = new JSONParser();
                JSONObject reader = (JSONObject) jsonParser.parse(new InputStreamReader(inputStream, "UTF-8"));
                json = reader.toString();
            } else if(field.getName().equals("namespace")){
                nameSpace = field.getValue();
            }
        }

        logger.debug("Json: {}", json);
        logger.debug("name: {}", name);
        logger.debug("namespace: {}", nameSpace);

        if(name.equals("") || nameSpace.equals("") || json.equals("")){
            throw new IllegalArgumentException("One of the given fields are wrong.");
        }


        classificationInstaller.create(name, nameSpace, json);
    }

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }
}
