package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.export.Export;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class ExportTest {
    Export export = new Export();

    @Test
    public void csvEscapeTest() {
        Assert.assertEquals("asdf", export.escapeCsv("asdf", ";"));
        Assert.assertEquals("\"as,df\"", export.escapeCsv("as,df", ";"));
        Assert.assertEquals("\"as;df\"", export.escapeCsv("as;df", ";"));
        Assert.assertEquals("\"as\"\"df\"", export.escapeCsv("as\"df", ";"));
        Assert.assertEquals("as|df", export.escapeCsv("as|df", ";"));
        Assert.assertEquals("\"as|df\"", export.escapeCsv("as|df", "|"));
        Assert.assertEquals("\"as\"\"|\"\"df\"", export.escapeCsv("as\"|\"df", "|"));
    }
}
