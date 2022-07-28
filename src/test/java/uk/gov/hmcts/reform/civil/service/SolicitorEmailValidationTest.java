package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@SpringBootTest(classes = {
    SolicitorEmailValidation.class,
    JacksonAutoConfiguration.class,
})
public class SolicitorEmailValidationTest {

    @Autowired
    private SolicitorEmailValidation solicitorEmailValidation;

    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";

    @Test
    void shouldMatchIfThereIsNoChangeInGAApplicantEmailAndCivilApplicantEmail_1V1() {

        CaseData caseData = solicitorEmailValidation
            .validateSolicitorEmail(getCivilCaseData(DUMMY_EMAIL, DUMMY_EMAIL, DUMMY_EMAIL), getGaCaseData(NO));

        assertThat(caseData.getGeneralAppApplnSolicitor().getEmail()).isEqualTo(DUMMY_EMAIL);
    }

    @Test
    void shouldMatchIfThereIsNoChangeInGARespondentEmailAndCivilRespondentEmail_1V1() {

        CaseData caseData = solicitorEmailValidation
            .validateSolicitorEmail(getCivilCaseData(DUMMY_EMAIL, DUMMY_EMAIL, DUMMY_EMAIL), getGaCaseData(NO));

        assertThat(caseData.getGeneralAppRespondentSolicitors().stream().findFirst().get().getValue().getEmail())
            .isEqualTo(DUMMY_EMAIL);
    }

    @Test
    void shouldMatchIfThereIsChangeInGAApplicantEmailAndCivilApplicantEmail_1V1() {

        CaseData caseData = solicitorEmailValidation
            .validateSolicitorEmail(
                getCivilCaseData("civilApplicant@gmail.com", DUMMY_EMAIL, DUMMY_EMAIL), getGaCaseData(NO));

        assertThat(caseData.getGeneralAppApplnSolicitor().getEmail()).isEqualTo("civilApplicant@gmail.com");
        assertThat(caseData.getGeneralAppRespondentSolicitors().stream().findFirst().get().getValue().getEmail())
            .isEqualTo(DUMMY_EMAIL);
    }

    @Test
    void shouldMatchIfThereIsChangeInGARespondentEmailAndCivilRespondentEmail_1V1() {

        CaseData caseData = solicitorEmailValidation
            .validateSolicitorEmail(
                getCivilCaseData(DUMMY_EMAIL,
                                 "civilrespondent1@gmail.com", DUMMY_EMAIL
                ), getGaCaseData(NO));

        assertThat(caseData.getGeneralAppApplnSolicitor().getEmail()).isEqualTo(DUMMY_EMAIL);
        assertThat(caseData.getGeneralAppRespondentSolicitors().stream().findFirst().get().getValue().getEmail())
            .isEqualTo("civilrespondent1@gmail.com");
    }

    @Test
    void shouldMatchIfThereIsChangeInGAApplicantEmailAndCivilApplicantEmail_2V1() {

        CaseData caseData = solicitorEmailValidation
            .validateSolicitorEmail(
                getCivilCaseData("civilApplicant@gmail.com", DUMMY_EMAIL, DUMMY_EMAIL), getGaCaseData(NO));

        assertThat(caseData.getGeneralAppApplnSolicitor().getEmail()).isEqualTo("civilApplicant@gmail.com");
        assertThat(caseData.getGeneralAppRespondentSolicitors().stream().findFirst().get().getValue().getEmail())
            .isEqualTo(DUMMY_EMAIL);
    }

    @Test
    void shouldMatchIfThereIsChangeInGARespondentEmailAndCivilRespondentEmail_2V1() {

        CaseData caseData = solicitorEmailValidation
            .validateSolicitorEmail(
                getCivilCaseData(DUMMY_EMAIL,
                                 "civilrespondent1@gmail.com", DUMMY_EMAIL
                ), getGaCaseData(NO));

        assertThat(caseData.getGeneralAppApplnSolicitor().getEmail()).isEqualTo(DUMMY_EMAIL);
        assertThat(caseData.getGeneralAppRespondentSolicitors().stream().findFirst().get().getValue().getEmail())
            .isEqualTo("civilrespondent1@gmail.com");
    }

    @Test
    void shouldMatchIfThereIsChangeInGAApplicantEmailAndCivilApplicantEmail_1V2() {

        CaseData caseData = solicitorEmailValidation
            .validateSolicitorEmail(
                getCivilCaseData("civilApplicant@gmail.com", DUMMY_EMAIL, DUMMY_EMAIL), getGaCaseData(YES));

        assertThat(caseData.getGeneralAppApplnSolicitor().getEmail()).isEqualTo("civilApplicant@gmail.com");
        assertThat(caseData.getGeneralAppRespondentSolicitors().stream().findFirst().get().getValue().getEmail())
            .isEqualTo(DUMMY_EMAIL);
    }

    @Test
    void shouldMatchIfThereIsChangeInGARespondent1EmailAndCivilRespondentEmail_1V2() {

        CaseData caseData = solicitorEmailValidation
            .validateSolicitorEmail(
                getCivilCaseData(DUMMY_EMAIL,
                                 "civilrespondent1@gmail.com", DUMMY_EMAIL
                ), getGaCaseData(YES));

        assertThat(caseData.getGeneralAppApplnSolicitor().getEmail()).isEqualTo(DUMMY_EMAIL);
        assertThat(checkIfThereIsMatchOrgIdAndEmailId(caseData.getGeneralAppRespondentSolicitors(),
                                                      "2", "civilrespondent1@gmail.com"
        )).isEqualTo(true);
    }

    @Test
    void shouldMatchIfThereIsChangeInGARespondent2EmailAndCivilRespondentEmail_1V2() {

        CaseData caseData = solicitorEmailValidation
            .validateSolicitorEmail(
                getCivilCaseData(DUMMY_EMAIL,
                                 DUMMY_EMAIL, "civilrespondent2@gmail.com"
                ), getGaCaseData(YES));

        assertThat(caseData.getGeneralAppApplnSolicitor().getEmail()).isEqualTo(DUMMY_EMAIL);
        assertThat(checkIfThereIsMatchOrgIdAndEmailId(caseData.getGeneralAppRespondentSolicitors(),
                                                      "3", "civilrespondent2@gmail.com"
        )).isEqualTo(true);
    }

    public boolean checkIfThereIsMatchOrgIdAndEmailId(List<Element<GASolicitorDetailsGAspec>>
                                                          generalAppRespondentSolicitors, String orgID, String email) {

        return generalAppRespondentSolicitors.stream().anyMatch(rs ->
                                                                    rs.getValue().getOrganisationIdentifier()
                                                                        .equals(orgID) && rs.getValue().getEmail()
                                                                        .equals(email));
    }

    private CaseData getCivilCaseData(String applicantEmail, String respondent1SolEmail, String respondent2SolEmail) {

        return new CaseDataBuilder()
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id").forename("GAApplnSolicitor")
                                          .email(DUMMY_EMAIL).organisationIdentifier("1").build())
            .respondentSolicitor1EmailAddress(respondent1SolEmail)
            .respondentSolicitor2EmailAddress(respondent2SolEmail)
            .applicantSolicitor1UserDetails(IdamUserDetails.builder()
                                                .id("123")
                                                .email(applicantEmail)
                                                .build())
            .applicant1OrganisationPolicy(OrganisationPolicy.builder()
                                              .organisation(Organisation.builder().organisationID("1").build())
                                              .build())
            .respondent1OrganisationPolicy(OrganisationPolicy.builder()
                                               .organisation(Organisation.builder().organisationID("2").build())
                                               .build())
            .respondent2SameLegalRepresentative(NO)
            .respondent2OrganisationPolicy(OrganisationPolicy.builder()
                                               .organisation(Organisation.builder().organisationID("3").build())
                                               .build())
            .build();
    }

    private CaseData getGaCaseData(YesOrNo isMultiParty) {

        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).forename("Respondent One").organisationIdentifier("2").build();

        GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).forename("Resondent Two").organisationIdentifier("3").build();

        respondentSols.add(element(respondent1));
        respondentSols.add(element(respondent2));

        return new CaseDataBuilder()
            .isMultiParty(isMultiParty)
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id").forename("Applicant One")
                                          .email(DUMMY_EMAIL).organisationIdentifier("1").build())
            .generalAppRespondentSolicitors(respondentSols)
            .parentClaimantIsApplicant(YES)
            .gaRespondentOrderAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .build();
    }
}
