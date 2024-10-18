package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.Language;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.RespondentLiPResponse;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GaForLipServiceTest {

    @Mock
    FeatureToggleService featureToggleService;
    @InjectMocks
    GaForLipService gaForLipService;

    @BeforeEach
    void setUp() {
        given(featureToggleService.isGaForLipsEnabled()).willReturn(true);
    }

    @Test
    void shouldReturnAnyTrue_app_is_welsh() {
        CaseData civilCaseData = CaseData.builder()
            .claimantBilingualLanguagePreference(Language.WELSH.name()).build();
        CaseData caseData = CaseData.builder().parentClaimantIsApplicant(YesOrNo.YES).build();
        assertThat(gaForLipService.anyWelsh(civilCaseData, caseData)).isTrue();
    }

    @Test
    void shouldReturnAnyTrue_resp_is_welsh() {
        CaseData civilCaseData = CaseData.builder()
            .respondent1LiPResponse(RespondentLiPResponse.builder()
                                        .respondent1ResponseLanguage(Language.WELSH.name()).build())
            .build();
        CaseData caseData = CaseData.builder().parentClaimantIsApplicant(YesOrNo.YES).build();
        assertThat(gaForLipService.anyWelsh(civilCaseData, caseData)).isTrue();
    }

    @Test
    void shouldReturnAnyFalse_nobody_is_welsh() {
        CaseData civilCaseData = CaseData.builder()
            .build();
        CaseData caseData = CaseData.builder().parentClaimantIsApplicant(YesOrNo.YES).build();
        assertThat(gaForLipService.anyWelsh(civilCaseData, caseData)).isFalse();
    }

    @Test
    void shouldReturnNoticeTrue_app_is_welsh() {
        CaseData civilCaseData = CaseData.builder()
            .claimantBilingualLanguagePreference(Language.WELSH.name()).build();
        CaseData caseData = CaseData.builder()
            .parentClaimantIsApplicant(YesOrNo.YES)
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YesOrNo.YES).build())
            .build();
        assertThat(gaForLipService.anyWelshNotice(civilCaseData, caseData)).isTrue();
    }

    @Test
    void shouldReturnNoticeTrue_resp_is_welsh() {
        CaseData civilCaseData = CaseData.builder()
            .respondent1LiPResponse(RespondentLiPResponse.builder()
                                        .respondent1ResponseLanguage(Language.WELSH.name()).build())
            .build();
        CaseData caseData = CaseData.builder()
            .parentClaimantIsApplicant(YesOrNo.YES)
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YesOrNo.YES).build())
            .build();
        assertThat(gaForLipService.anyWelshNotice(civilCaseData, caseData)).isTrue();
    }

    @Test
    void shouldReturnNoticeFalse_nobody_is_welsh() {
        CaseData civilCaseData = CaseData.builder()
            .build();
        CaseData caseData = CaseData.builder()
            .parentClaimantIsApplicant(YesOrNo.YES)
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YesOrNo.YES).build())
            .build();
        assertThat(gaForLipService.anyWelshNotice(civilCaseData, caseData)).isFalse();
    }

    @Test
    void shouldReturnWithoutNoticeFalse_resp_is_welsh() {
        CaseData civilCaseData = CaseData.builder()
            .respondent1LiPResponse(RespondentLiPResponse.builder()
                                        .respondent1ResponseLanguage(Language.WELSH.name()).build())
            .build();
        CaseData caseData = CaseData.builder()
            .parentClaimantIsApplicant(YesOrNo.YES)
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YesOrNo.NO).build())
            .build();
        assertThat(gaForLipService.anyWelshNotice(civilCaseData, caseData)).isFalse();
    }
}
