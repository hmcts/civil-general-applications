package uk.gov.hmcts.reform.civil.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.model.citizenui.dto.EventDto;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

@Tag(name = "CuiController")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(
    path = "/cases",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public class CuiController {

    private final CoreCaseDataService coreCaseDataService;

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
        CaseDetails caseDetails = coreCaseDataService.submitEventForCitizen(
            authorization,
            submitterId,
            eventDto.getEvent(),
            caseId,
            eventDto.getGeneralApplicationUpdate()
        );
        return new ResponseEntity<>(caseDetails, HttpStatus.OK);
    }

}
