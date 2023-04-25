package uk.gov.hmcts.reform.civil.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.model.ServiceRequestUpdateDto;
import uk.gov.hmcts.reform.civil.service.PaymentException;
import uk.gov.hmcts.reform.civil.service.PaymentRequestUpdateCallbackService;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentRequestUpdateCallbackController {

    private final PaymentRequestUpdateCallbackService requestUpdateCallbackService;

    @PutMapping(path = "/service-request-update", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @Operation(summary =  "Ways to pay will call this API and send the status of payment with other details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description  = "Callback processed.",
                content = { @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CallbackResponse.class)) }),
        @ApiResponse(responseCode = "400", description  = "Bad Request")})
    public void serviceRequestUpdate(@RequestBody ServiceRequestUpdateDto serviceRequestUpdateDto)
        throws PaymentException {
        try {
            requestUpdateCallbackService.processCallback(serviceRequestUpdateDto);
        } catch (Exception ex) {
            log.error(
                "Payment callback is unsuccessful for the CaseID: {}",
                serviceRequestUpdateDto.getCcdCaseNumber()
            );
            throw new PaymentException(ex.getMessage(), ex);
        }
    }

}
