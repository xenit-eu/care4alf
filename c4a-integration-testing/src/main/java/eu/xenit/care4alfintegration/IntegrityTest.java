package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.helpers.NodeHelper;
import eu.xenit.care4alf.integrity.FileProblem;
import eu.xenit.care4alf.integrity.IntegrityReport;
import eu.xenit.care4alf.integrity.IntegrityScanner;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class IntegrityTest {
    @Autowired
    IntegrityScanner integrityScanner;
    @Autowired
    ContentService contentService;
    @Autowired
    NodeService nodeService;
    @Autowired
    NodeHelper nodeHelper;

    @Test
    public void testRunScan() {
        // Verify we've scanned at least 800 nodes
        Assert.assertTrue("Expected at least 800 nodes scanned", integrityScanner.scanAll() > 800);
    }

    @Test
    public void testGetNodeReport() {
        integrityScanner.scanAll();
        Assert.assertNotNull("Expected report present after scan", integrityScanner.getLastReport());
        IntegrityReport report = integrityScanner.getLastReport();

        // Alfresco comes with a few node problems out of the box, e.g. missing mandatory property on these nodes
        NodeRef abeecher = new NodeRef("workspace://SpacesStore/dc103838-645f-43c1-8a2a-bc187e13c343");
        NodeRef mjackson = new NodeRef("workspace://SpacesStore/b6d80d49-21cc-4f04-9c92-e7063037543f");
        Map<NodeRef, ?> nodeProblems = report.getNodeProblems();
        Assert.assertTrue("Expected abeecher in nodes", nodeProblems.containsKey(abeecher));
        Assert.assertTrue("Expected mjackson in nodes", nodeProblems.containsKey(mjackson));
    }

    @Test
    public void testIncorrectEncodingSpecified() throws IOException {
        // Make a file with stated vs actual encoding mismatch
        NodeRef node = nodeHelper.createDocument(nodeHelper.getCompanyHome(), "utf8.txt");
        ContentWriter writer = contentService.getWriter(node, ContentModel.PROP_CONTENT, true);
        CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();
        WritableByteChannel channel = writer.getWritableChannel();
        channel.write(encoder.encode(CharBuffer.wrap("lingüística".toCharArray())));
        channel.close();
        ContentData contentData = (ContentData) nodeService.getProperty(node, ContentModel.PROP_CONTENT);
        ContentData updatedData= ContentData.setMimetype(ContentData.setEncoding(contentData, "UTF-8"), "text/plain");
        nodeService.setProperty(node, ContentModel.PROP_CONTENT, updatedData);

        // Verify report is present and has a file problem
        integrityScanner.scanAll();
        Assert.assertNotNull("Expected report present after scan", integrityScanner.getLastReport());
        IntegrityReport report = integrityScanner.getLastReport();

        Assert.assertNotEquals("Expected at least one file problem found", 0, report.getFileProblems().keySet().size());

        // Find the problem for the specific node we just made
        boolean foundOurNode = false;
        for (List<FileProblem> list : report.getFileProblems().values()) {
            for (FileProblem p : list) {
                if (p.getMessage().startsWith(node.toString())) {
                    foundOurNode = true;
                }
            }
        }

        Assert.assertTrue("Expected 1 incorrectly encoded file in FileProblems", foundOurNode);
    }

    @Test
    public void testMissingFile() throws IOException {
        // Make a node
        NodeRef node = nodeHelper.createDocument(nodeHelper.getCompanyHome(), "missingonfs.txt");
        ContentWriter writer = contentService.getWriter(node, ContentModel.PROP_CONTENT, true);
        writer.putContent("All these moments will be lost, like tears... in the rain.");
        ContentData contentData = (ContentData) nodeService.getProperty(node, ContentModel.PROP_CONTENT);
        String absolutePath = integrityScanner.absolutePath(contentData);
        Path path = Paths.get(absolutePath);
        // Delete the actual file
        Files.delete(path);

        // Verify report is present and has a file problem
        integrityScanner.scanAll();
        Assert.assertNotNull("Expected report present after scan", integrityScanner.getLastReport());
        IntegrityReport report = integrityScanner.getLastReport();

        Assert.assertNotEquals("Expected at least one file problem found", 0, report.getFileProblems().keySet().size());
        // Find the specific problem
        boolean foundOurNode = false;
        for (List<FileProblem> list : report.getFileProblems().values()) {
            for (FileProblem p : list) {
                if (p.getPath().equals(path.toString())) {
                    foundOurNode = true;
                }
            }
        }

        Assert.assertTrue("Expected node with missing file in FileProblems", foundOurNode);
    }
}
