package eu.xenit.care4alf.search;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class SolrSubsystemConditions {

    static String subsystemPropertyName = "index.subsystem.name";

    public class Solr1Condition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String subsystemVersion = context.getEnvironment().getProperty(subsystemPropertyName);
            return subsystemVersion != null && "solr".equals(subsystemVersion.toLowerCase());
        }
    }

    public class Solr4Condition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String subsystemVersion = context.getEnvironment().getProperty(subsystemPropertyName);
            return subsystemVersion != null && "solr4".equals(subsystemVersion.toLowerCase());
        }
    }

    public class Solr6Condition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String subsystemVersion = context.getEnvironment().getProperty(subsystemPropertyName);
            return subsystemVersion != null && "solr6".equals(subsystemVersion.toLowerCase());
        }
    }
}
