package xenit

import java.util

/**
 * @author Laurent Van der Linden
 */
package object care4alf {
    def JavaList[T](args: Iterable[T]) = {
        val list = new util.ArrayList[T]()
        for (arg <- args) list.add(arg)
        list
    }

    def JavaMap[A,B](elems: (A, B)*) = {
        val map = new java.util.HashMap[A,B]()
        for (elem <- elems) map.put(elem._1, elem._2)
        map
    }

}
