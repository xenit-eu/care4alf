package eu.xenit.care4alf.helpers;

import org.apache.commons.text.StringEscapeUtils;

public class StringEscapeUtilsWrapper {

    public static String escapeJava(String target) {
        return StringEscapeUtils.escapeJava(target);
    }

    public static String escapeJavaScript(String target) {
        return StringEscapeUtils.escapeEcmaScript(target);
    }

}
