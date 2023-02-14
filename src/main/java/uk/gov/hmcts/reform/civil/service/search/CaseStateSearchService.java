package uk.gov.hmcts.reform.civil.service.search;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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

    @Override
    Query queryForOrderMade_StayTheClaimCase(int startIndex, CaseState caseState) {
        MatchQueryBuilder queryCaseState = QueryBuilders.matchQuery("state", caseState.toString());
        MatchQueryBuilder queryGaType = QueryBuilders
            .matchQuery("data.generalAppType.types", "STAY_THE_CLAIM");
        MatchQueryBuilder queryOrderProcessStatus = QueryBuilders
            .matchQuery("data.judicialDecisionMakeOrder.isOrderProcessedByStayScheduler", "No");

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        query.must(queryCaseState).must(queryGaType).must(queryOrderProcessStatus);

        return new Query(
            query,
            emptyList(),
            startIndex
        );
    }

    @Override
    Query queryForOrderMade_UnlessOrderCase(int startIndex, CaseState caseState) {
        MatchQueryBuilder queryCaseState = QueryBuilders.matchQuery("state", caseState.toString());
        MatchQueryBuilder queryGaType = QueryBuilders
            .matchQuery("data.generalAppType.types", "UNLESS_ORDER");
        MatchQueryBuilder queryOrderProcessStatus = QueryBuilders
            .matchQuery("data.judicialDecisionMakeOrder.isOrderProcessedByUnlessScheduler", "No");

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        query.must(queryCaseState).must(queryGaType).must(queryOrderProcessStatus);

        return new Query(
            query,
            emptyList(),
            startIndex
        );
    }
}
