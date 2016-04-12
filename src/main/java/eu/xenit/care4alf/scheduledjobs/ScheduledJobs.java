package eu.xenit.care4alf.scheduledjobs;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import javax.management.*;
import javax.management.Attribute;
import java.io.IOException;
import java.util.Set;

/**
 * Created by willem on 6/1/15.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/scheduled", families = {"care4alf"}, description = "Show and execute scheduled jobs")
@Authentication(AuthenticationType.ADMIN)
public class ScheduledJobs {
    private final Logger logger = LoggerFactory.getLogger(ScheduledJobs.class);

    @Autowired
    private MBeanServerConnection alfrescoMBeanServer;
    public static final String[] ATTRIBUTES = new String[]{
            "CalendarName", "CronExpression", "Description", "EndTimeEndTime", "FinalFireTime", "Group", "JobGroup",
            "JobName", "MayFireAgain", "Name", "NextFireTime", "PreviousFireTime", "Priority", "StartTime", "State",
            "TimeZone", "Volatile"};

    @Uri(value="job")
    public void job(final WebScriptResponse response)
            throws IOException, JSONException, MalformedObjectNameException, ReflectionException,
            InstanceNotFoundException, IntrospectionException {
        Set<ObjectInstance> objects = alfrescoMBeanServer.queryMBeans(new ObjectName("Alfresco:Name=Schedule,Group=DEFAULT,Type=MonitoredCronTrigger,Trigger=*"),null);
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.array();
        for(ObjectInstance object : objects)
        {
            json.object();
            AttributeList attributeList = alfrescoMBeanServer.getAttributes(object.getObjectName(), ATTRIBUTES);
            for(Attribute attribute : attributeList.asList()){
                json.key(attribute.getName());
                json.value(attribute.getValue());
            }
            MBeanInfo info = alfrescoMBeanServer.getMBeanInfo(object.getObjectName());
            info.getOperations();
            json.key("actions");
            json.array();
            for(MBeanOperationInfo op : info.getOperations()){
                json.value(op.getName());
            }
            json.endArray();
            json.endObject();
        }
        json.endArray();
    }

    @Uri(value="job/{name}/execute", method = HttpMethod.POST)
    public void execute(@UriVariable final String name)
            throws IOException, JSONException, MalformedObjectNameException, InstanceNotFoundException, ReflectionException, MBeanException {
        String fullName = "Alfresco:Group=DEFAULT,Name=Schedule,Trigger="+name+",Type=MonitoredCronTrigger";
        String result = this.execute(fullName,"executeNow");
        logger.info("Result {}", result);
    }

    private String execute(String fullName, String operation){
        logger.info("Executing '{}'", fullName);
        logger.info(fullName);

        Object result = null;
        try {
            result = alfrescoMBeanServer.invoke(
                    new ObjectName(fullName),
                    operation,
                    null,
                    null
            );
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }

        if(result != null)
            return result.toString();

        return "no_result";
    }

    @Uri("validateschema")
    public void schemaValidation(WebScriptResponse res) throws IOException {
        res.getWriter().write(this.execute("Alfresco:Name=DatabaseInformation,Tool=SchemaValidator", "validateSchema"));
    }

}