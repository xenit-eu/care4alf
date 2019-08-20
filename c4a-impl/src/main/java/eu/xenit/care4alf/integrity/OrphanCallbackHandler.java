package eu.xenit.care4alf.integrity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import org.springframework.jdbc.core.RowCallbackHandler;

public class OrphanCallbackHandler implements RowCallbackHandler {
    // this is a reference to the given collection, i.e. modifying this also modifies the original.
    private Set<String> maybeOrphans;

    OrphanCallbackHandler(Set<String> maybeOrphans) {
        this.maybeOrphans = maybeOrphans;
    }

    @Override
    public void processRow(ResultSet rs) throws SQLException {
        maybeOrphans.remove(rs.getString("content_url"));
    }
}
