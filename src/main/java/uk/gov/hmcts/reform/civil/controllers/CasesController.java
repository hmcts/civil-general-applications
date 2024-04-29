package uk.gov.hmcts.reform.civil.controllers;


import static java.util.Collections.emptyList;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.civil.model.citizenui.dto.EventDto;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.citizen.events.CaseEventService;
import uk.gov.hmcts.reform.civil.service.citizen.events.EventSubmissionParams;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cases Controller")
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(
    path = "/cases",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public class CasesController {

    private final CoreCaseDataService coreCaseDataService;
    private final CaseEventService caseEventService;

    @PostMapping(path = "/")
    @Operation(summary = "get list of the cases from CCD")
    public ResponseEntity<SearchResult> getCaseList(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                    @RequestBody String searchString) {

        log.info("Received getCaseList");

        Query query = new Query(QueryBuilders
                .wrapperQuery(searchString), emptyList(), 0);
        SearchResult claims = coreCaseDataService.searchGeneralApplication(query, authorization);
        return new ResponseEntity<>(claims, HttpStatus.OK);
    }

    @GetMapping(path = {
            "/{caseId}",
    })
    @Operation(summary = "get case by id from CCD")
    public ResponseEntity<CaseDetails> getCaseId(
            @PathVariable("caseId") Long caseId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation
    ) {
        log.info(
                "Received CaseId: {}",
                caseId
        );

        var caseDetailsResponse = coreCaseDataService.getCase(caseId, authorisation);

        return new ResponseEntity<>(caseDetailsResponse, HttpStatus.OK);
    }

    @PostMapping(path = "/{caseId}/citizen/{submitterId}/event")
    @Operation(summary = "Submits event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Not Authorized")})
    public ResponseEntity<CaseDetails> submitEvent(
            @PathVariable("submitterId") String submitterId,
            @PathVariable("caseId") String caseId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody EventDto eventDto
    ) {
        EventSubmissionParams params = EventSubmissionParams
                .builder()
                .authorisation(authorization)
                .caseId(caseId)
                .userId(submitterId)
                .event(eventDto.getEvent())
                .updates(eventDto.getCaseDataUpdate())
                .build();
        CaseDetails caseDetails = caseEventService.submitEvent(params);
        return new ResponseEntity<>(caseDetails, HttpStatus.OK);
    }

}
