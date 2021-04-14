package eu.xenit.care4alf.search.subsystemconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class Solr4Condition implements Condition {

    static final Logger log = LoggerFactory.getLogger(Solr4Condition.class);

    static String subsystemPropertyName = "index.subsystem.name";

    public Solr4Condition() {}

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String subsystemVersion = context.getEnvironment().getProperty(subsystemPropertyName);
        log.info(subsystemVersion);
        return subsystemVersion != null && "solr4".equals(subsystemVersion.toLowerCase());
    }
}
