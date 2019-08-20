package eu.xenit.care4alf.module.bulk;

import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

/* This is a helper class for the MetadataWorkProvider and ActionCsvWorker.
 * It parses the kind of CSV file that should be uploaded in the metadata
 * option of the bulk processing. For reference, this file should have the
 * first line be the column header(s), with the first column representing the
 * noderef property that is searched for. The header is the name of the pro-
 * perty, the column contents are the values of the property. Additional
 * columns may be specified for extra data that is passed through the Action-
 * CsvWorker to the chosen Action. This Action will only receive the extra
 * data that relates to the noderef that it's working on, namely the data that
 * is on the same line as the property value by which the noderef was found.
 */
public class MetadataCSV {
    private final static Logger logger = LoggerFactory.getLogger(MetadataCSV.class);

    private InputStream content;
    private HashMap<String, String[]> csvContents;
    private String propertyName; // The header of the first column = the name of the property we search for
    private List<String> propertyValues;
    private String[] csvHeaders;

    public MetadataCSV(InputStream input) {
        this.content = input;
    }

    public Map<String, String[]> getCSVcontents() {
        if (csvContents == null) {
            initialize();
        }
        return csvContents;
    }

    private void initialize() {
        logger.debug("Reading csv file");
        Reader in = new InputStreamReader(content);
        try {
            csvContents = new HashMap<String, String[]>();
            propertyValues = new ArrayList<>();
            logger.debug("Creating parser");
            CSVParser parser = new CSVParser(in);
            // First line, headers
            logger.debug("Retrieving first line");
            String[] line = parser.getLine();
            logger.debug("First line is {}, property name is {}", line, line[0]);
            propertyName = line[0];
            csvHeaders = Arrays.copyOfRange(line, 1, line.length);
            // All other lines, contents
            logger.debug("Starting loop");
            while ((line = parser.getLine()) != null) {
                csvContents.put(line[0], Arrays.copyOfRange(line, 1, line.length));
                propertyValues.add(line[0]);
                logger.debug("Found {}", line[0]);
            }
            logger.debug("Done looping");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
    public String[] getCsvHeaders() {
        if (csvHeaders == null) {
            initialize();
        }
        return csvHeaders;
    }
    public List<String> getPropertyValues() {
        if (propertyValues == null) {
            initialize();
        }
        return propertyValues;
    }

    public String getPropertyName() {
        if (propertyName == null) {
            initialize();
        }
        return propertyName;
    }
}
