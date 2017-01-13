package eu.xenit.care4alf;

import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by willem on 1/13/17.
 */
@Component
public class DNSLookup {
    public List<String> getARecords(String host) throws NamingException {
        return this.getRecord(host, "A");
    }

    public List<String> getRecord(String host, String recordType) throws NamingException {
        List<String> values = new ArrayList<>();

        InitialDirContext iDirC = new InitialDirContext();
        javax.naming.directory.Attributes attributes = iDirC.getAttributes("dns:/" + host, new String[] {recordType});
        if(attributes.size() <= 0)
            return values;

        Attribute attributeMX = attributes.get(recordType);
        for (int i = 0; i < attributeMX.size(); i++)
        {
            values.add(attributeMX.get(i).toString());
        }

        return values;
    }

}
