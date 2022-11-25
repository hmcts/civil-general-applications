package uk.gov.hmcts.reform.civil.service.search;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.math.RoundingMode.UP;

@RequiredArgsConstructor
public abstract class ElasticSearchService {

    private final CoreCaseDataService coreCaseDataService;

    private static final int START_INDEX = 0;
    private static final int ES_DEFAULT_SEARCH_LIMIT = 10;

    public List<CaseDetails> getGeneralApplications(CaseState caseState) {
        SearchResult searchResult = coreCaseDataService.searchGeneralApplication(query(START_INDEX, caseState));
        int pages = calculatePages(searchResult);
        List<CaseDetails> caseDetails = new ArrayList<>(searchResult.getCases());

        for (int i = 1; i < pages; i++) {
            SearchResult result = coreCaseDataService
                .searchGeneralApplication(query(i * ES_DEFAULT_SEARCH_LIMIT, caseState));
            caseDetails.addAll(result.getCases());
        }

        return caseDetails;
    }

    abstract Query query(int startIndex, CaseState caseState);

    private int calculatePages(SearchResult searchResult) {
        return new BigDecimal(searchResult.getTotal()).divide(new BigDecimal(ES_DEFAULT_SEARCH_LIMIT), UP).intValue();
    }
}
