package eu.xenit.care4alfintegration;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.ClassPolicyDelegate;
import org.alfresco.repo.policy.PolicyComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 8/23/17.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class BehavioursTest {

    @Autowired
    PolicyComponent policyComponent;

    @Test
    public void listTest(){
        ClassPolicyDelegate<NodeServicePolicies.OnUpdatePropertiesPolicy> onUpdatePropertiesDelegate = policyComponent.registerClassPolicy(NodeServicePolicies.OnUpdatePropertiesPolicy.class);
        onUpdatePropertiesDelegate.getList(ContentModel.TYPE_CONTENT);
    }
}
