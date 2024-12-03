package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.model.OrganisationResponse;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.service.*;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICATION_UPDATE_AFTER_NOC;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.*;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationUpdateAfterNocCallbackHandler extends CallbackHandler {

    private static final int ONE_V_ONE = 1;
    private final ObjectMapper objectMapper;
    private final CoreCaseDataService coreCaseDataService;
    private final CoreCaseUserService coreCaseUserService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final GaForLipService gaForLipService;
    private final UserService userService;

    private static final List<CaseEvent> APPLICATION_UPDATE_AFTER_NOC_EVENTS =
        singletonList(APPLICATION_UPDATE_AFTER_NOC);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::updateApplicationDetailsAfterNoc
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return APPLICATION_UPDATE_AFTER_NOC_EVENTS;
    }

    private CallbackResponse updateApplicationDetailsAfterNoc(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        if(gaForLipService.isLipApp(caseData) && caseData.getParentClaimantIsApplicant()  == YesOrNo.YES) {
            if (civilCaseData.getApplicantSolicitor1UserDetails() != null) {
                coreCaseUserService.unassignCase(caseData.getCcdCaseReference().toString(),
                                                 caseData.getGeneralAppApplnSolicitor().getId(), null,
                                                 CLAIMANT);
                caseDataBuilder.isGaApplicantLip(YesOrNo.NO);
                caseDataBuilder.generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                                             .organisationIdentifier(civilCaseData.getApplicant1OrganisationPolicy().getOrganisation().getOrganisationID())
                                                             .email(civilCaseData.getApplicantSolicitor1UserDetails().getEmail())
                                                             .id(civilCaseData.getApplicantSolicitor1UserDetails().getId()).build());
                caseDataBuilder.applicant1OrganisationPolicy(OrganisationPolicy.builder()
                                                                 .organisation(Organisation.builder()
                                                                                   .organisationID(civilCaseData.getApplicant1OrganisationPolicy().getOrganisation().getOrganisationID())
                                                                                   .build())
                                                                 .orgPolicyCaseAssignedRole(APPLICANTSOLICITORONE.getFormattedName()).build());
                caseDataBuilder.civilServiceUserRoles(IdamUserDetails.builder().email(civilCaseData.getApplicantSolicitor1UserDetails().getEmail())
                                                          .id(civilCaseData.getApplicantSolicitor1UserDetails().getId()).build());

                String caseId = caseData.getCcdCaseReference().toString();
                String userId = civilCaseData.getApplicantSolicitor1UserDetails().getId();
                String organisationId = civilCaseData.getApplicant1OrganisationPolicy().getOrganisation().getOrganisationID();

                log.info("Assigning case to Applicant Solicitor One: {} and caseId: {}", userId, caseId);
                coreCaseUserService.assignCase(caseId, userId, organisationId, APPLICANTSOLICITORONE);

            }
        } else if (gaForLipService.isLipApp(caseData) && caseData.getParentClaimantIsApplicant()  == YesOrNo.NO
            && civilCaseData.getApplicantSolicitor1UserDetails() != null
            && caseData.getGeneralAppRespondentSolicitors().size() == ONE_V_ONE ) {
                coreCaseUserService.unassignCase(caseData.getCcdCaseReference().toString(),
                                                 caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getId(), null,
                                                 CaseRole.DEFENDANT);
                caseDataBuilder.isGaApplicantLip(YesOrNo.NO);
                List<Element<GASolicitorDetailsGAspec>> respSolUpdate = new ArrayList<>();
                respSolUpdate.add(element((GASolicitorDetailsGAspec.builder().email(civilCaseData.getApplicantSolicitor1UserDetails().getEmail())
                                      .id(civilCaseData.getApplicantSolicitor1UserDetails().getId()).build())));
                caseDataBuilder.generalAppRespondentSolicitors(respSolUpdate);
                caseDataBuilder
                .respondent1OrganisationPolicy(
                    OrganisationPolicy.builder()
                        .organisation(Organisation.builder()
                                          .organisationID(
                                             civilCaseData.getRespondent1OrganisationPolicy().getOrganisation().getOrganisationID()).build())
                        .orgPolicyCaseAssignedRole(RESPONDENTSOLICITORONE.getFormattedName()).build());

            }


        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

}
