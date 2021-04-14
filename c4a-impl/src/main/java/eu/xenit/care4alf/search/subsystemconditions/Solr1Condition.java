package eu.xenit.care4alf.search.subsystemconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class Solr1Condition implements Condition {

    static final Logger log = LoggerFactory.getLogger(Solr1Condition.class);

    static String subsystemPropertyName = "index.subsystem.name";

    public Solr1Condition() {}

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String subsystemVersion = context.getEnvironment().getProperty(subsystemPropertyName);
        log.info(subsystemVersion);
        return subsystemVersion != null && "solr".equals(subsystemVersion.toLowerCase());
    }
}
