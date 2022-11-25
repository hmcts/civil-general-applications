package uk.gov.hmcts.reform.civil.service.search;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

@Service
public class CaseStateSearchService extends ElasticSearchService {

    public CaseStateSearchService(CoreCaseDataService coreCaseDataService) {
        super(coreCaseDataService);
    }

    @Override
    public Query query(int startIndex, CaseState caseState) {

        return new Query(
            matchQuery("state", caseState.toString()),
            emptyList(),
            startIndex
        );
    }

}
