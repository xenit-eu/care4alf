package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.DNSLookup;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by willem on 1/13/17.
 */
@Component
public class DNSLookupTest {
    DNSLookup dnsLookup = new DNSLookup();

    @Test
    public void testGetARecords() throws NamingException, UnknownHostException {
        List<String> values = dnsLookup.getARecords("c4atest.dev.xenit.eu");
        Assert.assertEquals(2,values.size());
        List<String> expected = new ArrayList<>();
        expected.add("144.76.74.76");
        expected.add("144.76.74.77");
        Assert.assertTrue(values.containsAll(expected) && values.containsAll(expected));
    }

    @Test
    public void testCnameRecord() throws NamingException, UnknownHostException {
        Assert.assertEquals(0, dnsLookup.getRecord("c4atest.dev.xenit.eu", "CNAME").size());
    }

}