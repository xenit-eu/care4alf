package eu.xenit.care4alf.unittest;

import eu.xenit.care4alf.audit.Audit;
import org.junit.Assert;
import org.junit.Test;

public class AuditTest {
    @Test
    public void testDecodeQnamePath() throws Exception {
        String qnamePath = "/app:company_home/app:guest_home/cm:_x0031_23_x0020_picture.png";
        Assert.assertEquals("/app:company_home/app:guest_home/cm:123 picture.png", Audit.decodeNodePath(qnamePath));
    }
}
