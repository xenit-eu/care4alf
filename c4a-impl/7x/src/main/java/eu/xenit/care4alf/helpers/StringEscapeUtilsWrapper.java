package eu.xenit.care4alf.helpers;

import org.apache.commons.text.StringEscapeUtils;

public class StringEscapeUtilsWrapper {

    public static String escapeJava(String target) {
        return StringEscapeUtils.escapeJava(target);
    }

}
