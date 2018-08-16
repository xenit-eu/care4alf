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
        List<String> values = dnsLookup.getARecords("google-public-dns-a.google.com");
        Assert.assertEquals(1,values.size());
        List<String> expected = new ArrayList<>();
        expected.add("8.8.8.8");
        Assert.assertTrue(values.containsAll(expected) && values.containsAll(expected));
    }

    @Test
    public void testCnameRecord() throws NamingException, UnknownHostException {
        Assert.assertEquals(0, dnsLookup.getRecord("c4atest.dev.xenit.eu", "CNAME").size());
    }

}