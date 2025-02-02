package uk.gov.hmcts.reform.civil.controllers.testingsupport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;
import uk.gov.hmcts.reform.civil.service.OrganisationService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.civil.model.OrganisationResponse;

import java.util.Optional;

@Tag(name = "AssignCaseSupportController")
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(
    path = "/testing-support",
    produces = MediaType.APPLICATION_JSON_VALUE,
    consumes = MediaType.APPLICATION_JSON_VALUE
)
@ConditionalOnExpression("${testing.support.enabled:false}")
public class AssignCaseSupportController {

    private final CoreCaseUserService coreCaseUserService;
    private final IdamClient idamClient;
    private final OrganisationService organisationService;

    @PostMapping(value = {"/assign-case/{caseId}", "/assign-case/{caseId}/{caseRole}"})
    @Operation(summary = "Assign case to user")
    public void assignCase(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
                           @PathVariable("caseId") String caseId,
                           @PathVariable("caseRole") Optional<CaseRole> caseRole) {
        log.info("Assign caseId: {}", caseId);
        String userId = idamClient.getUserInfo(authorisation).getUid();
        boolean isCitizen = !caseRole.map(CaseRole::isProfessionalRole).orElse(false);

        String organisationId = isCitizen ? null : organisationService.findOrganisation(authorisation)
            .map(OrganisationResponse::getOrganisationIdentifier).orElse(null);

        coreCaseUserService.assignCase(
            caseId,
            userId,
            organisationId,
            caseRole.orElse(CaseRole.RESPONDENTSOLICITORONE)
        );
    }
}
