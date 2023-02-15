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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public List<CaseDetails> getOrderMadeGeneralApplications(CaseState caseState) {

        // Search General Application contains Stay the Claim
        SearchResult searchStayClaimResult = coreCaseDataService
            .searchGeneralApplication(queryForOrderMade_StayTheClaimCase(START_INDEX, caseState));

        int pages = calculatePages(searchStayClaimResult);
        List<CaseDetails> caseDetailsStayClaim = new ArrayList<>(searchStayClaimResult.getCases());

        for (int i = 1; i < pages; i++) {
            SearchResult result = coreCaseDataService
                .searchGeneralApplication(queryForOrderMade_StayTheClaimCase(i * ES_DEFAULT_SEARCH_LIMIT,
                                                                             caseState));
            caseDetailsStayClaim.addAll(result.getCases());
        }

        // Search General application contains Unless Order
        SearchResult searchUnlessOrderResult = coreCaseDataService
            .searchGeneralApplication(queryForOrderMade_UnlessOrderCase(START_INDEX, caseState));

        int pagesUnlessOrder = calculatePages(searchUnlessOrderResult);
        List<CaseDetails> caseDetailsUnlessOrder = new ArrayList<>(searchUnlessOrderResult.getCases());

        for (int i = 1; i < pagesUnlessOrder; i++) {
            SearchResult resultUnlessOrder = coreCaseDataService
                .searchGeneralApplication(queryForOrderMade_UnlessOrderCase(i * ES_DEFAULT_SEARCH_LIMIT,
                                                                             caseState));
            caseDetailsUnlessOrder.addAll(resultUnlessOrder.getCases());
        }
        caseDetailsUnlessOrder.removeAll(caseDetailsStayClaim);
        List<CaseDetails> caseDetails = Stream.concat(caseDetailsStayClaim.stream(),
                                                      caseDetailsUnlessOrder.stream()).collect(Collectors.toList());

        return caseDetails;
    }

    abstract Query query(int startIndex, CaseState caseState);

    abstract Query queryForOrderMade_StayTheClaimCase(int startIndex, CaseState caseState);

    abstract Query queryForOrderMade_UnlessOrderCase(int startIndex, CaseState caseState);

    private int calculatePages(SearchResult searchResult) {
        return new BigDecimal(searchResult.getTotal()).divide(new BigDecimal(ES_DEFAULT_SEARCH_LIMIT), UP).intValue();
    }
}
