package eu.xenit.care4alf.permissionimport.reader;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class XlsxReaderTest {

    @Test
    public void testForLoop() throws IOException {
        File file = new File("src/test/resources/permissionimport/test.xlsx");
        InputStream is = new FileInputStream(file);
        PermissionReader permissionReader = new XlsxPermissionReader(is);

        List<PermissionSetting> permissionSettings = new ArrayList<>();

        for (PermissionSetting permissionSetting: permissionReader) {
            permissionSettings.add(permissionSetting);
        }

        System.out.println("Number of permissionSettings = "+permissionSettings.size());
        assertTrue("No permissions found in xlsx", permissionSettings.size()==4);
        assertEquals("test", permissionSettings.get(0).getPath()[0]);
        assertEquals("folder1", permissionSettings.get(0).getPath()[1]);
    }

}
