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
}
