package uk.gov.hmcts.reform.civil.service.search;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static uk.gov.hmcts.reform.civil.enums.CaseState.*;

@Service
public class DirectionOrderSearchService extends ElasticSearchService {

    public DirectionOrderSearchService(CoreCaseDataService coreCaseDataService) {
        super(coreCaseDataService);
    }

    public Query query(int startIndex) {

        return new Query(
            matchQuery("state", AWAITING_DIRECTIONS_ORDER_DOCS.toString()),
            emptyList(),
            startIndex
        );
    }
}
