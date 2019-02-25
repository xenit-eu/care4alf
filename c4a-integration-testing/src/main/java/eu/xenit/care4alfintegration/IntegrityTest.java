package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.integrity.IntegrityScanner;
import eu.xenit.care4alf.integrity.IntegrityReport;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import java.util.Map;
import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class IntegrityTest {
    @Autowired
    IntegrityScanner integrityScanner;

    @Test
    public void testRunScan() throws InterruptedException {
        // Verify we've scanned at least 800 nodes
        Assert.assertTrue("At least 800 nodes scanned", integrityScanner.scanAll() > 800);
    }

    @Test
    public void testGetNodeReport() {
        integrityScanner.scanAll();
        Assert.assertNotNull("Report present after scan", integrityScanner.getLastReport());
        IntegrityReport report = integrityScanner.getLastReport();

        NodeRef abeecher = new NodeRef("workspace://SpacesStore/dc103838-645f-43c1-8a2a-bc187e13c343");
        NodeRef mjackson = new NodeRef("workspace://SpacesStore/b6d80d49-21cc-4f04-9c92-e7063037543f");
        Map<NodeRef, ?> nodeProblems = report.getNodeProblems();
        Assert.assertTrue("abeecher in nodes", nodeProblems.containsKey(abeecher));
        Assert.assertTrue("mjackson in nodes", nodeProblems.containsKey(mjackson));
    }

    @Ignore
    @Test
    public void testGetFileReport() {
        integrityScanner.scanAll();
        Assert.assertNotNull("Report present after scan", integrityScanner.getLastReport());
        IntegrityReport report = integrityScanner.getLastReport();

        // generate file problems

        Assert.assertEquals("At least one file problem found", 1, report.getFileProblems().keySet().size());
    }
}
