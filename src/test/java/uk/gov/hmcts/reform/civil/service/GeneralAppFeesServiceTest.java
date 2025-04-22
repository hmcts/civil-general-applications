package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.civil.client.FeesApiClient;
import uk.gov.hmcts.reform.civil.config.GeneralAppFeesConfiguration;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDateGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneralAppFeesServiceTest {

    private static final String APPLICATION_TO_VARY_OR_SUSPEND = "AppnToVaryOrSuspend";
    private static final String CERT_OF_SATISFACTION_OR_CANCEL = "CertificateOfSorC";
    private static final String GENERAL_APP_WITHOUT_NOTICE = "GeneralAppWithoutNotice";
    private static final String GENERAL_APPLICATION_WITH_NOTICE = "GAOnNotice";
    public static final String DEFAULT_CHANNEL = "default";
    public static final String CIVIL_JURISDICTION = "civil";
    public static final String TEST_FEE_CODE = "test_fee_code";

    private static final BigDecimal TEST_FEE_AMOUNT_POUNDS = new BigDecimal("108.00");

    private static final BigDecimal TEST_FEE_AMOUNT_POUNDS_15 = new BigDecimal("15.00");

    private static final BigDecimal TEST_FEE_AMOUNT_PENCE = new BigDecimal(TEST_FEE_AMOUNT_POUNDS.intValue() * 100);

    private static final BigDecimal TEST_FEE_AMOUNT_PENCE_15 = new BigDecimal("1500");

    private static final FeeLookupResponseDto FEE_POUNDS = FeeLookupResponseDto.builder()
        .feeAmount(TEST_FEE_AMOUNT_POUNDS).code(TEST_FEE_CODE).version(1).build();

    private static final FeeLookupResponseDto FEE_POUNDS_15 = FeeLookupResponseDto.builder()
        .feeAmount(TEST_FEE_AMOUNT_POUNDS_15).code("test_fee_code").version(1).build();
    private static final Fee FEE_PENCE = Fee.builder()
        .calculatedAmountInPence(TEST_FEE_AMOUNT_PENCE).code(TEST_FEE_CODE).version("1").build();

    private static final Fee FEE_PENCE_15 = Fee.builder()
        .calculatedAmountInPence(TEST_FEE_AMOUNT_PENCE_15).code("test_fee_code").version("1").build();
    public static final String FREE_REF = "FREE";
    private static final Fee FEE_PENCE_0 = Fee.builder()
        .calculatedAmountInPence(BigDecimal.ZERO).code(FREE_REF).version("1").build();
    public static final String GENERAL_APPLICATION = "general application";
    public static final String GENERAL_SERVICE = "general";

    @Captor
    private ArgumentCaptor<String> keywordCaptor;

    @Mock
    private FeesApiClient feesApiClient;

    @Mock
    private GeneralAppFeesConfiguration feesConfiguration;

    @InjectMocks
    private GeneralAppFeesService generalAppFeesService;

    @Nested
    class FeeForGA {

        @ParameterizedTest
        @CsvSource({
            "GAOnNotice, NO, YES",
            "GeneralAppWithoutNotice, NO, NO"
        })
        void default_types_should_pay_ForGA(String noticeType, YesOrNo hasAgreed, YesOrNo isWithNotice) {
            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                keywordCaptor.capture()
            )).thenReturn(FEE_POUNDS);
            when(feesConfiguration.getService()).thenReturn(GENERAL_SERVICE);
            when(feesConfiguration.getChannel()).thenReturn(DEFAULT_CHANNEL);
            when(feesConfiguration.getJurisdiction1()).thenReturn(CIVIL_JURISDICTION);
            when(feesConfiguration.getJurisdiction2()).thenReturn(CIVIL_JURISDICTION);
            when(feesConfiguration.getEvent()).thenReturn(GENERAL_APPLICATION);

            if (GENERAL_APPLICATION_WITH_NOTICE.equals(noticeType)) {
                when(feesConfiguration.getWithNoticeKeyword()).thenReturn(GENERAL_APPLICATION_WITH_NOTICE);
            } else {
                when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn(GENERAL_APP_WITHOUT_NOTICE);
            }
            List<GeneralApplicationTypes> allTypes = getGADefaultTypes();
            //single
            for (GeneralApplicationTypes generalApplicationType : allTypes) {
                CaseData caseData = getFeeCase(
                    List.of(generalApplicationType), hasAgreed, isWithNotice, null);
                Fee feeDto = generalAppFeesService.getFeeForGA(caseData);
                assertThat(feeDto).isEqualTo(FEE_PENCE);
            }
            //mix
            CaseData caseData = getFeeCase(
                allTypes, hasAgreed, isWithNotice, null);
            Fee feeDto = generalAppFeesService.getFeeForGA(caseData);
            assertThat(feeDto).isEqualTo(FEE_PENCE);
            assertThat(keywordCaptor.getValue())
                .hasToString(noticeType);
        }

        static Stream<Arguments> adjourn_with_hearingScheduledDate_outside_14daysData() {
            return Stream.of(
                Arguments.of(GeneralApplicationTypes.ADJOURN_HEARING, YesOrNo.YES, YesOrNo.YES, 15, FEE_PENCE_0),
                Arguments.of(GeneralApplicationTypes.ADJOURN_HEARING, YesOrNo.YES, YesOrNo.NO, 15, FEE_PENCE_0)
            );
        }

        @ParameterizedTest
        @MethodSource(
            "adjourn_with_hearingScheduledDate_outside_14daysData"
        )
        void adjourn_with_hearingScheduledDate_outside_14days_should_pay_no_fee(GeneralApplicationTypes generalApplicationTypes,
                                                                                YesOrNo hasAgreed,
                                                                                YesOrNo isWithNotice,
                                                                                Integer daysToAdd,
                                                                                Fee expectedFee) {

            CaseData caseData = getFeeCase(
                List.of(generalApplicationTypes),
                hasAgreed, isWithNotice, LocalDate.now().plusDays(daysToAdd)
            );
            Fee feeForGA = generalAppFeesService.getFeeForGA(caseData);
            assertThat(feeForGA)
                .isEqualTo(expectedFee);
        }

        static Stream<Arguments> generateDefaultTypesData() {
            return Stream.of(
                Arguments.of(GeneralApplicationTypes.VARY_PAYMENT_TERMS_OF_JUDGMENT, YesOrNo.YES, YesOrNo.YES, -1, FEE_PENCE),
                Arguments.of(GeneralApplicationTypes.VARY_PAYMENT_TERMS_OF_JUDGMENT, YesOrNo.NO, YesOrNo.NO, -1, FEE_PENCE),
                Arguments.of(GeneralApplicationTypes.SETTLE_BY_CONSENT, YesOrNo.YES, YesOrNo.YES, -1, FEE_PENCE),
                Arguments.of(GeneralApplicationTypes.SET_ASIDE_JUDGEMENT, YesOrNo.YES, YesOrNo.YES, -1, FEE_PENCE),
                Arguments.of(GeneralApplicationTypes.SET_ASIDE_JUDGEMENT, YesOrNo.NO, YesOrNo.YES, -1, FEE_PENCE),
                Arguments.of(GeneralApplicationTypes.ADJOURN_HEARING, YesOrNo.NO, YesOrNo.YES, 1, FEE_PENCE),
                Arguments.of(GeneralApplicationTypes.ADJOURN_HEARING, YesOrNo.NO, YesOrNo.NO, 1, FEE_PENCE),
                Arguments.of(GeneralApplicationTypes.ADJOURN_HEARING, YesOrNo.NO, YesOrNo.NO, 15, FEE_PENCE)
            );
        }

        @ParameterizedTest
        @MethodSource(
            "generateDefaultTypesData"
        )
        void default_types_should_pay(GeneralApplicationTypes generalApplicationTypes, YesOrNo hasAgreed, YesOrNo isWithNotice, Integer daysToAdd, Fee expectedFee) {
            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                keywordCaptor.capture()
            )).thenReturn(FEE_POUNDS);

            when(feesConfiguration.getChannel()).thenReturn(DEFAULT_CHANNEL);
            when(feesConfiguration.getJurisdiction1()).thenReturn(CIVIL_JURISDICTION);
            when(feesConfiguration.getJurisdiction2()).thenReturn(CIVIL_JURISDICTION);

            if (generalApplicationTypes == GeneralApplicationTypes.VARY_PAYMENT_TERMS_OF_JUDGMENT) {
                when(feesConfiguration.getAppnToVaryOrSuspend()).thenReturn(APPLICATION_TO_VARY_OR_SUSPEND);
            } else {
                when(feesConfiguration.getService()).thenReturn(GENERAL_SERVICE);
                when(feesConfiguration.getEvent()).thenReturn(GENERAL_APPLICATION);
            }

            if (generalApplicationTypes == GeneralApplicationTypes.SETTLE_BY_CONSENT) {
                when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn(GENERAL_APP_WITHOUT_NOTICE);
            } else if (generalApplicationTypes == GeneralApplicationTypes.SET_ASIDE_JUDGEMENT && hasAgreed == YesOrNo.NO) {
                if (isWithNotice == YesOrNo.YES) {
                    when(feesConfiguration.getWithNoticeKeyword()).thenReturn(GENERAL_APPLICATION_WITH_NOTICE);
                } else if (isWithNotice == YesOrNo.NO) {
                    when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn(GENERAL_APP_WITHOUT_NOTICE);
                }
            } else if (generalApplicationTypes == GeneralApplicationTypes.ADJOURN_HEARING) {
                if (isWithNotice == YesOrNo.YES) {
                    when(feesConfiguration.getWithNoticeKeyword()).thenReturn(GENERAL_APPLICATION_WITH_NOTICE);
                } else if (isWithNotice == YesOrNo.NO) {
                    when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn(GENERAL_APP_WITHOUT_NOTICE);
                }
            }

            LocalDate noOfDays = daysToAdd > 0 ? LocalDate.now().plusDays(daysToAdd) : null;
            CaseData caseDataWithNotice = getFeeCase(
                List.of(generalApplicationTypes),
                hasAgreed, isWithNotice, noOfDays
            );
            Fee feeDto = generalAppFeesService.getFeeForGA(caseDataWithNotice);
            assertThat(feeDto).isEqualTo(expectedFee);
        }

        static Stream<Arguments> mixDefaultTypesData() {
            return Stream.of(
                Arguments.of(GeneralApplicationTypes.SET_ASIDE_JUDGEMENT, YesOrNo.YES, YesOrNo.YES, 15, FEE_PENCE),
                Arguments.of(GeneralApplicationTypes.VARY_PAYMENT_TERMS_OF_JUDGMENT, YesOrNo.YES, YesOrNo.YES, 15, FEE_PENCE)
            );
        }

        @ParameterizedTest
        @MethodSource(
            "mixDefaultTypesData"
        )
        void mix_default_types_should_be_charged_a_fee(GeneralApplicationTypes generalApplicationTypes,
                                                       YesOrNo hasAgreed,
                                                       YesOrNo isWithNotice,
                                                       Integer daysToAdd,
                                                       Fee expectedFee) {

            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                keywordCaptor.capture()
            ))
                .thenReturn(FEE_POUNDS);
            when(feesConfiguration.getService()).thenReturn(GENERAL_SERVICE);
            when(feesConfiguration.getChannel()).thenReturn(DEFAULT_CHANNEL);
            when(feesConfiguration.getJurisdiction1()).thenReturn(CIVIL_JURISDICTION);
            when(feesConfiguration.getJurisdiction2()).thenReturn(CIVIL_JURISDICTION);
            when(feesConfiguration.getEvent()).thenReturn(GENERAL_APPLICATION);
            when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn(GENERAL_APP_WITHOUT_NOTICE);

            if (generalApplicationTypes == GeneralApplicationTypes.SET_ASIDE_JUDGEMENT && hasAgreed == YesOrNo.NO) {
                when(feesConfiguration.getWithNoticeKeyword()).thenReturn(GENERAL_APPLICATION_WITH_NOTICE);
            } else if (generalApplicationTypes == GeneralApplicationTypes.VARY_PAYMENT_TERMS_OF_JUDGMENT) {
                when(feesConfiguration.getAppnToVaryOrSuspend()).thenReturn(APPLICATION_TO_VARY_OR_SUSPEND);
            }

            LocalDate noOfDays = daysToAdd > 0 ? LocalDate.now().plusDays(daysToAdd) : null;
            List<GeneralApplicationTypes> randomList = getGADefaultTypes();
            randomList.add(generalApplicationTypes);
            CaseData caseDataOutside14Days = getFeeCase(
                randomList,
                hasAgreed, isWithNotice, noOfDays
            );
            assertThat(generalAppFeesService.getFeeForGA(caseDataOutside14Days))
                .isEqualTo(expectedFee);
            List<String> keywords = keywordCaptor.getAllValues();
            assertThat(keywords).contains(GENERAL_APP_WITHOUT_NOTICE);
            if (generalApplicationTypes == GeneralApplicationTypes.SET_ASIDE_JUDGEMENT && hasAgreed == YesOrNo.NO) {
                assertThat(keywords).contains(GENERAL_APPLICATION_WITH_NOTICE);
            } else if (generalApplicationTypes == GeneralApplicationTypes.VARY_PAYMENT_TERMS_OF_JUDGMENT) {
                assertThat(keywords).contains(APPLICATION_TO_VARY_OR_SUSPEND);
            }
        }
    }

    @Test
    void shouldReturnFeeData_whenCertificateOfSatisfactionOrCancelRequested() {
        when(feesConfiguration.getChannel()).thenReturn("default");
        when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
        when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
        when(feesConfiguration.getCertificateOfSatisfaction()).thenReturn("CertificateOfSorC");

        when(feesApiClient.lookupFee(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(CERT_OF_SATISFACTION_OR_CANCEL)
        )).thenReturn(FEE_POUNDS_15);

        CaseData caseData = getFeeCase(
            List.of(GeneralApplicationTypes.CONFIRM_CCJ_DEBT_PAID), YesOrNo.NO, YesOrNo.NO, null);
        Fee feeDto = generalAppFeesService.getFeeForGA(caseData);
        assertThat(feeDto).isEqualTo(FEE_PENCE_15);
    }

    private CaseData getFeeCase(List<GeneralApplicationTypes> types, YesOrNo hasAgreed,
                                YesOrNo isWithNotice, LocalDate hearingScheduledDate) {
        CaseData.CaseDataBuilder builder = CaseData.builder();
        builder.generalAppType(GAApplicationType.builder().types(types).build());
        if (Objects.nonNull(hasAgreed)) {
            builder.generalAppRespondentAgreement(GARespondentOrderAgreement
                                                      .builder().hasAgreed(hasAgreed).build());
        }
        if (Objects.nonNull(isWithNotice)) {
            builder.generalAppInformOtherParty(
                GAInformOtherParty.builder().isWithNotice(isWithNotice).build());
        }
        if (Objects.nonNull(hearingScheduledDate)) {
            builder.generalAppHearingDate(GAHearingDateGAspec.builder()
                                              .hearingScheduledDate(hearingScheduledDate).build());
        }
        return builder.build();
    }

    private List<GeneralApplicationTypes> getGADefaultTypes() {
        List<GeneralApplicationTypes> allTypes =
            Stream.of(GeneralApplicationTypes.values()).collect(Collectors.toList());
        allTypes.removeAll(GeneralAppFeesService.VARY_TYPES);
        allTypes.removeAll(GeneralAppFeesService.SET_ASIDE);
        allTypes.removeAll(GeneralAppFeesService.ADJOURN_TYPES);
        allTypes.removeAll(GeneralAppFeesService.SD_CONSENT_TYPES);
        allTypes.removeAll(GeneralAppFeesService.CONFIRM_YOU_PAID_CCJ_DEBT);
        Collections.shuffle(allTypes);
        return allTypes;
    }
}
