package eu.xenit.care4alfintegration.permissionimport.reader;

import eu.xenit.care4alf.permissionimport.reader.PermissionReader;
import eu.xenit.care4alf.permissionimport.reader.PermissionSetting;
import eu.xenit.care4alf.permissionimport.reader.XlsxPermissionReader;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XlsxReaderTest {

    @Test
    public void testForLoop() throws IOException {
        File file = new File("src/main/resources/permissionimport/test.xlsx");
        InputStream is = new FileInputStream(file);
        PermissionReader permissionReader = new XlsxPermissionReader(is);

        List<PermissionSetting> permissionSettings = new ArrayList<>();

        for (PermissionSetting permissionSetting: permissionReader) {
            permissionSettings.add(permissionSetting);
        }

        System.out.println("Number of permissionSettings = "+permissionSettings.size());
        assertTrue("No permissions found in xlsx", permissionSettings.size()==5);
        assertEquals("test", permissionSettings.get(0).getPath()[0]);
        assertEquals("folder1", permissionSettings.get(0).getPath()[1]);
    }

}
