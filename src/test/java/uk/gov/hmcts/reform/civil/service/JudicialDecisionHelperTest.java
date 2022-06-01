package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@SpringBootTest(classes = {
    JudicialDecisionHelper.class,
})
public class JudicialDecisionHelperTest {

    @Autowired
    private JudicialDecisionHelper helper;

    @Nested
    class IsApplicationCloakedTests {

        @Test
        void isApplicationCloaked_shouldReturnNoWhenRespondentAgreementIsNull() {
            CaseData caseData = CaseData.builder().generalAppRespondentAgreement(null).build();
            assertThat(helper.isApplicationCloaked(caseData)).isEqualTo(NO);
        }

        @Test
        void isApplicationCloaked_shouldReturnNoWhenRespondentAgreementHasAgreed() {
            CaseData caseData = CaseData.builder().generalAppRespondentAgreement(
                GARespondentOrderAgreement.builder().hasAgreed(YES).build()).build();
            assertThat(helper.isApplicationCloaked(caseData)).isEqualTo(NO);
        }

        @Test
        void isApplicationCloaked_shouldReturnNoWhenRespondentAgreementHasNotAgreedButNotificationDetailsNotSet() {
            CaseData caseData = CaseData.builder().generalAppRespondentAgreement(
                    GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .generalAppInformOtherParty(null).build();
            assertThat(helper.isApplicationCloaked(caseData)).isEqualTo(NO);
        }

        @Test
        void isApplicationCloaked_shouldReturnNoWhenRespondentAgreementHasNotAgreedAndNotified() {
            CaseData caseData = CaseData.builder().generalAppRespondentAgreement(
                    GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build()).build();
            assertThat(helper.isApplicationCloaked(caseData)).isEqualTo(NO);
        }

        @Test
        void isApplicationCloaked_shouldReturnNoWhenRespondentAgreementHasNotAgreedAndUnNotified() {
            CaseData caseData = CaseData.builder().generalAppRespondentAgreement(
                    GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(NO).build()).build();
            assertThat(helper.isApplicationCloaked(caseData)).isEqualTo(YES);
        }
    }

    @Nested
    class IsApplicantAndRespondentLocationPrefSame {

        @Test
        void shouldReturnFalse_whenApplicantHearingDetailsNotProvided() {
            CaseData caseData = CaseData.builder().build();
            assertThat(helper.isApplicantAndRespondentLocationPrefSame(caseData)).isEqualTo(false);
        }

        @Test
        void shouldReturnFalse_whenRespondentHearingDetailsNotProvided() {
            CaseData caseData = CaseData.builder().generalAppHearingDetails(
                GAHearingDetails.builder().build()).build();
            assertThat(helper.isApplicantAndRespondentLocationPrefSame(caseData)).isEqualTo(false);
        }

        @Test
        void shouldReturnFalse_whenLocationSelectedByApplicantAndOneOfTheRespondentIsNotSame() {
            CaseData caseData = CaseData.builder()
                .generalAppHearingDetails(
                    GAHearingDetails.builder()
                        .hearingPreferredLocation(
                            getDynamicLocationsList("ABCD - RG0 0AL",
                                                    getDynamicLocation("ABCD - RG0 0AL"),
                                                    getDynamicLocation("PQRS - GU0 0EE"),
                                                    getDynamicLocation("WXYZ - EW0 0HE"),
                                                    getDynamicLocation("LMNO - NE0 0BH"))).build())
                .respondentsResponses(
                    wrapElements(
                        GARespondentResponse.builder()
                            .gaHearingDetails(
                                GAHearingDetails.builder()
                                    .hearingPreferredLocation(
                                        getDynamicLocationsList("PQRS - GU0 0EE",
                                                                getDynamicLocation("ABCD - RG0 0AL"),
                                                                getDynamicLocation("PQRS - GU0 0EE"),
                                                                getDynamicLocation("WXYZ - EW0 0HE"),
                                                                getDynamicLocation("LMNO - NE0 0BH"))).build())
                            .build(),
                        GARespondentResponse.builder()
                            .gaHearingDetails(
                                GAHearingDetails.builder()
                                    .hearingPreferredLocation(
                                        getDynamicLocationsList("ABCD - RG0 0AL",
                                                                getDynamicLocation("ABCD - RG0 0AL"),
                                                                getDynamicLocation("PQRS - GU0 0EE"),
                                                                getDynamicLocation("WXYZ - EW0 0HE"),
                                                                getDynamicLocation("LMNO - NE0 0BH"))).build())
                            .build()
                    )
                )
                .build();
            assertThat(helper.isApplicantAndRespondentLocationPrefSame(caseData)).isEqualTo(false);
        }

        @Test
        void shouldReturnFalse_whenLocationSelectedByApplicantAndBothOfTheRespondentIsNotSame() {
            CaseData caseData = CaseData.builder()
                .generalAppHearingDetails(
                    GAHearingDetails.builder()
                        .hearingPreferredLocation(
                            getDynamicLocationsList("ABCD - RG0 0AL",
                                                    getDynamicLocation("ABCD - RG0 0AL"),
                                                    getDynamicLocation("PQRS - GU0 0EE"),
                                                    getDynamicLocation("WXYZ - EW0 0HE"),
                                                    getDynamicLocation("LMNO - NE0 0BH"))).build())
                .respondentsResponses(
                    wrapElements(
                        GARespondentResponse.builder()
                            .gaHearingDetails(
                                GAHearingDetails.builder()
                                    .hearingPreferredLocation(
                                        getDynamicLocationsList("PQRS - GU0 0EE",
                                                                getDynamicLocation("ABCD - RG0 0AL"),
                                                                getDynamicLocation("PQRS - GU0 0EE"),
                                                                getDynamicLocation("WXYZ - EW0 0HE"),
                                                                getDynamicLocation("LMNO - NE0 0BH"))).build())
                            .build(),
                        GARespondentResponse.builder()
                            .gaHearingDetails(
                                GAHearingDetails.builder()
                                    .hearingPreferredLocation(
                                        getDynamicLocationsList("WXYZ - EW0 0HE",
                                                                getDynamicLocation("ABCD - RG0 0AL"),
                                                                getDynamicLocation("PQRS - GU0 0EE"),
                                                                getDynamicLocation("WXYZ - EW0 0HE"),
                                                                getDynamicLocation("LMNO - NE0 0BH"))).build())
                            .build()
                    )
                )
                .build();
            assertThat(helper.isApplicantAndRespondentLocationPrefSame(caseData)).isEqualTo(false);
        }

        @Test
        void shouldReturnTrue_whenLocationSelectedByApplicantAndBothRespondentIsSame() {
            CaseData caseData = CaseData.builder()
                .generalAppHearingDetails(
                    GAHearingDetails.builder()
                        .hearingPreferredLocation(
                            getDynamicLocationsList("ABCD - RG0 0AL",
                                                    getDynamicLocation("ABCD - RG0 0AL"),
                                                    getDynamicLocation("PQRS - GU0 0EE"),
                                                    getDynamicLocation("WXYZ - EW0 0HE"),
                                                    getDynamicLocation("LMNO - NE0 0BH"))).build())
                .respondentsResponses(
                    wrapElements(
                        GARespondentResponse.builder()
                            .gaHearingDetails(
                                GAHearingDetails.builder()
                                    .hearingPreferredLocation(
                                        getDynamicLocationsList("ABCD - RG0 0AL",
                                                                getDynamicLocation("ABCD - RG0 0AL"),
                                                                getDynamicLocation("PQRS - GU0 0EE"),
                                                                getDynamicLocation("WXYZ - EW0 0HE"),
                                                                getDynamicLocation("LMNO - NE0 0BH"))).build())
                            .build(),
                        GARespondentResponse.builder()
                            .gaHearingDetails(
                                GAHearingDetails.builder()
                                    .hearingPreferredLocation(
                                        getDynamicLocationsList("ABCD - RG0 0AL",
                                                                getDynamicLocation("ABCD - RG0 0AL"),
                                                                getDynamicLocation("PQRS - GU0 0EE"),
                                                                getDynamicLocation("WXYZ - EW0 0HE"),
                                                                getDynamicLocation("LMNO - NE0 0BH"))).build())
                            .build()
                        )
                )
                .build();
            assertThat(helper.isApplicantAndRespondentLocationPrefSame(caseData)).isEqualTo(true);
        }

        private DynamicListElement getDynamicLocation(String label) {
            return DynamicListElement.builder()
                .code(UUID.randomUUID()).label(label).build();
        }

        private DynamicList getDynamicLocationsList(String chosenValue, DynamicListElement... elements) {
            Optional<DynamicListElement> first = Arrays.stream(elements)
                .filter(e -> e.getLabel().equals(chosenValue)).findFirst();
            DynamicList.DynamicListBuilder dynamicListBuilder = DynamicList.builder()
                .listItems(List.of(elements));
            first.ifPresent(dynamicListBuilder::value);
            return dynamicListBuilder.build();
        }
    }
}

