package eu.xenit.care4alf.search.subsystemconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class Solr6Condition implements Condition {

    static String subsystemPropertyName = "index.subsystem.name";

    static final Logger log = LoggerFactory.getLogger(Solr6Condition.class);

    public Solr6Condition() {}

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
//        String subsystemVersion = context.getEnvironment().getProperty(subsystemPropertyName);
//        log.info(subsystemVersion);
//        return subsystemVersion != null && "solr6".equals(subsystemVersion.toLowerCase());
        return true;
    }
}