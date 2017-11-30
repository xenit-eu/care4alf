package eu.xenit.care4alfintegration;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.Attributes;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * Created by willem on 5/1/16.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class AttributesTest {
    @Autowired
    Attributes attributes;

    @Autowired
    RetryingTransactionHelper retryingTransactionHelper;

    @Test
    public void testListNotEmpty() throws Exception {
        Assert.assertTrue(this.attributes.list().size() >= 1);
    }

    @Test
    public void testCreateAndExists() throws SQLException {
        boolean failed = false;
        try {
            retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>() {
                @Override
                public NodeRef execute() throws Throwable {
                    attributes.create(new String[]{"integrationtest"}, "OK");
                    return null;
                }
            }, false, true);
        }catch (Exception e){
            e.printStackTrace();
            failed = true;
            Assert.fail(e.getMessage());
        }finally {
            Boolean found = retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Boolean>() {
                @Override
                public Boolean execute() throws Throwable {
                    for(Attributes.Attribute a : attributes.list()){
                        if(a.getKey1().equals("integrationtest")) {
                            Assert.assertTrue(true);
                            return true;
                        }
                    }
                    return false;
                }
            }, false, true);
            if (!failed) {
                Assert.assertTrue(found);
            }
        }
    }

    @Test
    public void testRemove() throws SQLException {
        retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Object>() {
            @Override
            public NodeRef execute() throws Throwable {
                attributes.remove(new String[]{"integrationtest"});
                return null;
            }
        }, false, true);

        retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Object>() {
            @Override
            public NodeRef execute() throws Throwable {
                boolean found = false;
                for(Attributes.Attribute a : attributes.list()){
                    if(a.getKey1().equals("integrationtest")) {
                        found = true;
                        Assert.assertFalse(true);
                    }
                }
                Assert.assertFalse(found);
                return null;
            }
        }, false, true);
    }

}
