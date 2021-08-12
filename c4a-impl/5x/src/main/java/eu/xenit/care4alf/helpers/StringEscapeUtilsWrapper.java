package eu.xenit.care4alf.helpers;

import org.apache.commons.lang.StringEscapeUtils;

public class StringEscapeUtilsWrapper {

    public static String escapeJava(String target) {
        return StringEscapeUtils.escapeJava(target);
    }

}
