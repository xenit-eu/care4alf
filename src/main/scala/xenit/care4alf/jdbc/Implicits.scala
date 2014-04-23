package xenit.care4alf.jdbc

import java.sql.ResultSet
import org.springframework.jdbc.core.RowMapper
import scala.language.implicitConversions

/**
 * @author Laurent Van der Linden
 */
object Implicits {
    implicit def functionAsRowMapper[T](mapperFunction: (ResultSet) => T) = new RowMapper[T] {
        def mapRow(rs: ResultSet, rowNum: Int): T = mapperFunction(rs)
    }
}
