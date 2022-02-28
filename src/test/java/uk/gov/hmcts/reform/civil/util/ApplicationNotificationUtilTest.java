package uk.gov.hmcts.reform.civil.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil.isNotificationCriteriaSatisfied;

public class ApplicationNotificationUtilTest {

    @Test
    void testIsNotificationCriteriaSatisfied() {
        assertThat(isNotificationCriteriaSatisfied(getCaseData(NO, NO, NO, null))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(NO, NO, NO, "respondent@email.com"))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(NO, NO, YES, null))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(NO, NO, YES, "respondent@email.com"))).isTrue();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(NO, YES, NO, null))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(NO, YES, NO, "respondent@email.com"))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(NO, YES, YES, null))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(NO, YES, YES, "respondent@email.com"))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(YES, NO, NO, null))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(YES, NO, NO, "respondent@email.com"))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(YES, NO, YES,  null))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(YES, NO, YES,  "respondent@email.com"))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(YES, YES, NO, null))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(YES, YES, NO, "respondent@email.com"))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(YES, YES, YES, null))).isFalse();
        assertThat(isNotificationCriteriaSatisfied(getCaseData(YES, YES, YES, "respondent@email.com"))).isFalse();
    }

    private CaseData getCaseData(YesOrNo isConsented, YesOrNo isUrgent, YesOrNo informOtherParty, String recipient) {
        return CaseData.builder()
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(isConsented).build())
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(isUrgent).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(informOtherParty).build())
                .respondentSolicitor1EmailAddress(recipient)
                .build();
    }
}
