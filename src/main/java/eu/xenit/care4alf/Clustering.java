package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.management.*;
import java.io.IOException;

/**
 * Created by willem on 7/14/16.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/clustering", families = {"care4alf"}, description = "Clustering")
public class Clustering {
    @Autowired
    private MBeanServerConnection alfrescoMBeanServer;

    public int getNumClusterMembers(){
        try {
            AttributeList attributes = alfrescoMBeanServer.getAttributes(new ObjectName("Alfresco:Name=Cluster,Tool=Admin"), new String[]{"NumClusterMembers"});
            int numClusterMembers = (int) ((Attribute) attributes.get(0)).getValue();
            return numClusterMembers;
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean testCluster(){
        return false;
    }
}