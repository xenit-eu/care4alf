package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.export.Export;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.stereotype.Component;

@Component
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
