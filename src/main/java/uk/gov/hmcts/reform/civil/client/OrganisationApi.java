package uk.gov.hmcts.reform.civil.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.civil.model.OrganisationResponse;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi.SERVICE_AUTHORIZATION;

@FeignClient(name = "rd-professional-api", url = "${rd_professional.api.url}")
public interface OrganisationApi {

    @GetMapping("/refdata/external/v1/organisations")
    OrganisationResponse findUserOrganisation(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization
    );

    @GetMapping("/refdata/internal/v1/organisations")
    OrganisationResponse findOrganisationById(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestParam("id") String organisationId
    );

    @GetMapping("/refdata/internal/v1/organisations/orgDetails/{userId}")
    OrganisationResponse findOrganisationByUserId(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @PathVariable("userId") String userId
    );

}
