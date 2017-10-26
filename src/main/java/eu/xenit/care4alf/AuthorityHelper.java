package eu.xenit.care4alf;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;

import java.util.List;

public class AuthorityHelper {

    private final SearchService searchService;

    public AuthorityHelper(SearchService searchService) {
        this.searchService = searchService;
    }


    public List<NodeRef> getNodeGroupNodeRefs(String authorityDisplayName) {
        SearchParameters authoritySearchParameters = new SearchParameters();
        authoritySearchParameters.setQueryConsistency(QueryConsistency.TRANSACTIONAL);
        authoritySearchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        authoritySearchParameters.setMaxItems(2);
        authoritySearchParameters.setQuery("=TYPE:\"cm:authorityContainer\" AND =cm:authorityDisplayName:\""+authorityDisplayName+"\"");
        authoritySearchParameters.setLanguage(SearchService. LANGUAGE_FTS_ALFRESCO);
        ResultSet groupAuthorityResultSet = searchService.query(authoritySearchParameters);
        return groupAuthorityResultSet.getNodeRefs();
    }

    public List<NodeRef> getUserNodeRefs(String user) {
        SearchParameters userSearchParameters = new SearchParameters();
        userSearchParameters.setQueryConsistency(QueryConsistency.TRANSACTIONAL);
        userSearchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        userSearchParameters.setMaxItems(2);
        userSearchParameters.setQuery("=TYPE:\"cm:person\" AND =cm:userName:\""+user+"\"");
        userSearchParameters.setLanguage(SearchService. LANGUAGE_FTS_ALFRESCO);
        ResultSet userAuthorityResultSet = searchService.query(userSearchParameters);
        return userAuthorityResultSet.getNodeRefs();
    }
}
