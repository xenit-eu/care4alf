package eu.xenit.care4alf.helpers;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;

public class StringUtilsWrapper {

    public static String join(Collection collection, String separator) {
        return StringUtils.join(collection, separator);
    }

}
