package eu.xenit.care4alf.integrity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.alfresco.service.cmr.repository.NodeRef;

public class NoderefFieldSerializer extends JsonSerializer<NodeRef> {
    private ObjectMapper objectMapper = new ObjectMapper();
//    public NodeProblemsSerializer() {
//        this(null);
//    }

//    @Override
//    public void serialize(Class<Map<NodeRef, NodeProblem>> value, JsonGenerator jgen, SerializerProvider provider)
//            throws IOException, JsonProcessingException {
//
//    }

//    public NodeProblemsSerializer(Class<Map<NodeRef, NodeProblem>> t) {
//        super(t);
//    }


    @Override
    public void serialize(NodeRef value, JsonGenerator generator, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        generator.writeFieldName(value.toString());
    }
}
