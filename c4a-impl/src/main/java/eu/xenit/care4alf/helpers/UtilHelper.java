package eu.xenit.care4alf.helpers;

import java.util.HashMap;

/**
 * Created by Thomas.Straetmans on 11/05/2017.
 */
public class UtilHelper {

    public static Object getOrElse(HashMap map, Object key, Object def){
        Object o = map.get(key);
        return(o == null ? def : o);
    }

    public static String codepointToString(int cp) {
        StringBuilder sb = new StringBuilder();
        if (Character.isBmpCodePoint(cp)) {
            sb.append((char) cp);
        } else if (Character.isValidCodePoint(cp)) {
            sb.append(Character.highSurrogate(cp));
            sb.append(Character.lowSurrogate(cp));
        } else {
            // invalid sequence, just insert a replacement character
            sb.append("\uFFFD");
        }
        return sb.toString();
    }
}
