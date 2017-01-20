package eu.xenit.care4alf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.management.*;
import java.io.IOException;

/**
 * Created by willem on 7/14/16.
 */
@Component
public class Clustering {
    @Autowired
    private MBeanServerConnection alfrescoMBeanServer;

    public int getNumClusterMembers(){
        try {
            AttributeList attributes = alfrescoMBeanServer.getAttributes(new ObjectName("Alfresco:Name=Cluster,Tool=Admin"), new String[]{"NumClusterMembers"});
            int numClusterMembers = (Integer) ((Attribute) attributes.get(0)).getValue();
            return numClusterMembers;
        } catch (InstanceNotFoundException e) {
            //e.printStackTrace();
        } catch (ReflectionException e) {
//            e.printStackTrace();
        } catch (IOException e) {
//            e.printStackTrace();
        } catch (MalformedObjectNameException e) {
//            e.printStackTrace();
        } catch(Exception e){
//            e.printStackTrace();
        }
        return -1;
    }

}