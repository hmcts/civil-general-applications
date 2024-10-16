package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.civil.client.FeesApiClient;
import uk.gov.hmcts.reform.civil.config.GeneralAppFeesConfiguration;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDateGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.ADJOURN_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.RELIEF_FROM_SANCTIONS;

@ExtendWith(MockitoExtension.class)
class GeneralAppFeesServiceTest {

    private static final BigDecimal TEST_FEE_AMOUNT_POUNDS_108 = new BigDecimal("108.00");
    private static final BigDecimal TEST_FEE_AMOUNT_PENCE_108 = new BigDecimal("10800");
    private static final BigDecimal TEST_FEE_AMOUNT_POUNDS_275 = new BigDecimal("275.00");
    private static final BigDecimal TEST_FEE_AMOUNT_PENCE_275 = new BigDecimal("27500");
    private static final BigDecimal TEST_FEE_AMOUNT_POUNDS_14 = new BigDecimal("14.00");
    private static final BigDecimal TEST_FEE_AMOUNT_PENCE_14 = new BigDecimal("1400");
    private static final BigDecimal TEST_FEE_AMOUNT_POUNDS_15 = new BigDecimal("15.00");
    private static final BigDecimal TEST_FEE_AMOUNT_PENCE_15 = new BigDecimal("1500");
    private static final String AppnToVaryOrSuspend = "AppnToVaryOrSuspend";
    private static final String CERT_OF_SATISFACTION_OR_CANCEL = "CertificateOfSorC";
    private static final String WithoutNotice = "GeneralAppWithoutNotice";
    private static final String GAOnNotice = "GAOnNotice";
    private static final String GAFree = "CopyPagesUpTo10";
    private static final FeeLookupResponseDto FEE_POUNDS_108 = FeeLookupResponseDto.builder()
        .feeAmount(TEST_FEE_AMOUNT_POUNDS_108).code("test_fee_code").version(1).build();
    private static final Fee FEE_PENCE_108 = Fee.builder()
        .calculatedAmountInPence(TEST_FEE_AMOUNT_PENCE_108).code("test_fee_code").version("1").build();
    private static final FeeLookupResponseDto FEE_POUNDS_275 = FeeLookupResponseDto.builder()
        .feeAmount(TEST_FEE_AMOUNT_POUNDS_275).code("test_fee_code").version(1).build();
    private static final Fee FEE_PENCE_275 = Fee.builder()
        .calculatedAmountInPence(TEST_FEE_AMOUNT_PENCE_275).code("test_fee_code").version("1").build();
    private static final FeeLookupResponseDto FEE_POUNDS_14 = FeeLookupResponseDto.builder()
        .feeAmount(TEST_FEE_AMOUNT_POUNDS_14).code("test_fee_code").version(2).build();
    private static final Fee FEE_PENCE_14 = Fee.builder()
        .calculatedAmountInPence(TEST_FEE_AMOUNT_PENCE_14).code("test_fee_code").version("2").build();
    private static final FeeLookupResponseDto FEE_POUNDS_0 = FeeLookupResponseDto.builder()
        .feeAmount(BigDecimal.ZERO).code("test_fee_code").version(2).build();
    public static final String FREE_REF = "FREE";
    private static final Fee FEE_PENCE_0 = Fee.builder()
        .calculatedAmountInPence(BigDecimal.ZERO).code(FREE_REF).version("1").build();
    private static final FeeLookupResponseDto FEE_POUNDS_15 = FeeLookupResponseDto.builder()
        .feeAmount(TEST_FEE_AMOUNT_POUNDS_15).code("test_fee_code").version(1).build();
    private static final Fee FEE_PENCE_15 = Fee.builder()
        .calculatedAmountInPence(TEST_FEE_AMOUNT_PENCE_15).code("test_fee_code").version("1").build();

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Mock
    private FeesApiClient feesApiClient;

    @Mock
    private GeneralAppFeesConfiguration feesConfiguration;

    @InjectMocks
    private GeneralAppFeesService feesService;

    @Test
    void shouldReturnFeeData_whenConsentedApplicationIsBeingMadeForVaryAppln() {
        when(feesConfiguration.getChannel()).thenReturn("default");
        when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
        when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
        when(feesConfiguration.getAppnToVaryOrSuspend()).thenReturn("AppnToVaryOrSuspend");

        when(feesApiClient.lookupFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                                     stringArgumentCaptor.capture()
        ))
            .thenReturn(FEE_POUNDS_108);

        Fee feeDto = feesService.getFeeForGA(feesConfiguration.getAppnToVaryOrSuspend(), "miscellaneous", "other");

        assertThat(feeDto).isEqualTo(FEE_PENCE_108);
        assertThat(stringArgumentCaptor.getValue().toString())
            .hasToString(AppnToVaryOrSuspend);
    }

    @Test
    void shouldReturnFeeData_whenConsentedApplicationIsBeingMade() {
        when(feesConfiguration.getEvent()).thenReturn("event");
        when(feesConfiguration.getService()).thenReturn("general");
        when(feesConfiguration.getChannel()).thenReturn("default");
        when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
        when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
        when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn("GeneralAppWithoutNotice");

        when(feesApiClient.lookupFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                                     stringArgumentCaptor.capture()
        ))
            .thenReturn(FEE_POUNDS_108);

        Fee feeDto = feesService.getFeeForGA(feesConfiguration.getConsentedOrWithoutNoticeKeyword(), null, null);

        assertThat(feeDto).isEqualTo(FEE_PENCE_108);
        assertThat(stringArgumentCaptor.getValue().toString())
            .hasToString(WithoutNotice);
    }

    @Test
    void shouldReturnFeeData_whenUnonsentedWithoutNoticeApplicationIsBeingMade() {
        when(feesConfiguration.getEvent()).thenReturn("event");
        when(feesConfiguration.getService()).thenReturn("general");
        when(feesConfiguration.getChannel()).thenReturn("default");
        when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
        when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
        when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn("GeneralAppWithoutNotice");

        when(feesApiClient.lookupFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                                     stringArgumentCaptor.capture()
        ))
            .thenReturn(FEE_POUNDS_108);

        Fee feeDto = feesService.getFeeForGA(feesConfiguration.getConsentedOrWithoutNoticeKeyword(), null, null);

        assertThat(feeDto).isEqualTo(FEE_PENCE_108);
        assertThat(stringArgumentCaptor.getValue().toString())
            .hasToString(WithoutNotice);
    }

    @Test
    void shouldReturnFeeData_whenUnconsentedNotifiedApplicationIsBeingMade() {
        when(feesConfiguration.getEvent()).thenReturn("event");
        when(feesConfiguration.getService()).thenReturn("general");
        when(feesConfiguration.getChannel()).thenReturn("default");
        when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
        when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
        when(feesConfiguration.getWithNoticeKeyword()).thenReturn("GAOnNotice");

        when(feesApiClient.lookupFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                                     stringArgumentCaptor.capture()
        ))
            .thenReturn(FEE_POUNDS_275);

        Fee feeDto = feesService.getFeeForGA(feesConfiguration.getWithNoticeKeyword(), null, null);

        assertThat(feeDto).isEqualTo(FEE_PENCE_275);
        verify(feesConfiguration, times(1)).getWithNoticeKeyword();
        verify(feesConfiguration, never()).getConsentedOrWithoutNoticeKeyword();
        assertThat(stringArgumentCaptor.getValue().toString())
            .hasToString(GAOnNotice);
    }

    @Test
    void shouldPay_whenIsNotAdjournVacateApplication() {

        CaseData caseData = new CaseDataBuilder()
            .requestForInformationApplication()
            .build();
        assertThat(feesService.isFreeApplication(caseData)).isFalse();
        GeneralApplication ga =
            adjournOrVacateHearingApplication(
                YesOrNo.YES,
                LocalDate.now().plusDays(15)
            )
                .generalAppType(GAApplicationType
                                    .builder().types(List.of(RELIEF_FROM_SANCTIONS)).build())
                .build();
        assertThat(feesService.isFreeGa(ga)).isFalse();
    }

    @Test
    void shouldPay_whenConsentedWithin14DaysAdjournVacateApplicationIsBeingMade() {

        CaseData caseData = new CaseDataBuilder()
            .adjournOrVacateHearingApplication(YesOrNo.YES, LocalDate.now().plusDays(14))
            .build();
        assertThat(feesService.isFreeApplication(caseData)).isFalse();
        GeneralApplication ga =
            adjournOrVacateHearingApplication(
                YesOrNo.YES,
                LocalDate.now().plusDays(14)
            ).build();
        assertThat(feesService.isFreeGa(ga)).isFalse();
    }

    @Test
    void shouldBeFree_whenConsentedLateThan14DaysAdjournVacateApplicationIsBeingMade() {
        CaseData caseData = new CaseDataBuilder()
            .adjournOrVacateHearingApplication(YesOrNo.YES, LocalDate.now().plusDays(15))
            .build();
        assertThat(feesService.isFreeApplication(caseData)).isTrue();
        GeneralApplication ga =
            adjournOrVacateHearingApplication(
                YesOrNo.YES,
                LocalDate.now().plusDays(15)
            ).build();
        assertThat(feesService.isFreeGa(ga)).isTrue();
    }

    @Test
    void throwRuntimeException_whenFeeServiceThrowsException() {
        when(feesConfiguration.getService()).thenReturn("general");
        when(feesConfiguration.getEvent()).thenReturn("event");
        when(feesConfiguration.getChannel()).thenReturn("default");
        when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
        when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
        when(feesConfiguration.getWithNoticeKeyword()).thenReturn("GAOnNotice");

        when(feesApiClient.lookupFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                                     stringArgumentCaptor.capture()
        ))
            .thenThrow(new RuntimeException("Some Exception"));
        String keyword = feesConfiguration.getWithNoticeKeyword();
        Exception exception = assertThrows(RuntimeException.class, () -> feesService
            .getFeeForGA(keyword, null, null));

        assertThat(exception.getMessage()).isEqualTo("java.lang.RuntimeException: Some Exception");
    }

    @Test
    void throwRuntimeException_whenNoFeeIsReturnedByFeeService() {
        when(feesConfiguration.getService()).thenReturn("general");
        when(feesConfiguration.getEvent()).thenReturn("event");
        when(feesConfiguration.getChannel()).thenReturn("default");
        when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
        when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
        when(feesConfiguration.getWithNoticeKeyword()).thenReturn("GAOnNotice");

        when(feesApiClient.lookupFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                                     stringArgumentCaptor.capture()
        ))
            .thenReturn(null);
        String keyword = feesConfiguration.getWithNoticeKeyword();
        Exception exception = assertThrows(RuntimeException.class, () -> feesService
            .getFeeForGA(keyword, null, null));

        assertThat(exception.getMessage())
            .isEqualTo("No Fees returned by fee-service while creating General Application");
    }

    @Test
    void throwRuntimeException_whenNoFeeAmountIsReturnedByFeeService() {
        when(feesConfiguration.getEvent()).thenReturn("event");
        when(feesConfiguration.getService()).thenReturn("general");
        when(feesConfiguration.getChannel()).thenReturn("default");
        when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
        when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
        when(feesConfiguration.getWithNoticeKeyword()).thenReturn("GAOnNotice");

        when(feesApiClient.lookupFee(anyString(), anyString(), anyString(), anyString(), anyString(),
                                     anyString()
        ))
            .thenReturn(FeeLookupResponseDto.builder()
                            .code("test_fee_code")
                            .version(1)
                            .build());
        String keyword = feesConfiguration.getWithNoticeKeyword();
        Exception exception = assertThrows(RuntimeException.class, () -> feesService
            .getFeeForGA(keyword, null, null));

        assertThat(exception.getMessage())
            .isEqualTo("No Fees returned by fee-service while creating General Application");
    }

    private GeneralApplication.GeneralApplicationBuilder adjournOrVacateHearingApplication(
        YesOrNo isRespondentAgreed, LocalDate gaHearingDate) {
        GAHearingDateGAspec generalAppHearingDate = GAHearingDateGAspec.builder()
            .hearingScheduledDate(gaHearingDate)
            .build();
        return GeneralApplication.builder()
            .claimant1PartyName("Test Claimant1 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY)
                                 .build())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(ADJOURN_HEARING))
                                .build())
            .generalAppHearingDate(generalAppHearingDate)
            .generalAppRespondentAgreement(GARespondentOrderAgreement
                                               .builder().hasAgreed(isRespondentAgreed).build());
    }

    @Nested
    class LowestFee {

        @Test
        void default_types_with_notice_should_pay_275() {
            when(feesConfiguration.getEvent()).thenReturn("event");
            when(feesConfiguration.getService()).thenReturn("general");
            when(feesConfiguration.getChannel()).thenReturn("default");
            when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
            when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
            when(feesConfiguration.getWithNoticeKeyword()).thenReturn("GAOnNotice");
            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(GAOnNotice)
            )).thenReturn(FEE_POUNDS_275);

            List<GeneralApplicationTypes> allTypes =
                Stream.of(GeneralApplicationTypes.values()).collect(Collectors.toList());
            allTypes.removeAll(GeneralAppFeesService.VARY_TYPES);
            allTypes.removeAll(GeneralAppFeesService.SET_ASIDE);
            allTypes.removeAll(GeneralAppFeesService.ADJOURN_TYPES);
            allTypes.removeAll(GeneralAppFeesService.SD_CONSENT_TYPES);
            allTypes.removeAll(GeneralAppFeesService.CONFIRM_YOU_PAID_CCJ_DEBT);
            //single
            for (GeneralApplicationTypes generalApplicationType : allTypes) {
                CaseData caseData = getFeeCase(
                    List.of(generalApplicationType), YesOrNo.NO, YesOrNo.YES, null);
                Fee feeDto = feesService.getFeeForGA(caseData);
                assertThat(feeDto).isEqualTo(FEE_PENCE_275);
            }
            //mix
            CaseData caseData = getFeeCase(
                allTypes, YesOrNo.NO, YesOrNo.YES, null);
            Fee feeDto = feesService.getFeeForGA(caseData);
            assertThat(feeDto).isEqualTo(FEE_PENCE_275);
        }

        @Test
        void default_types_without_notice_should_pay_108() {
            when(feesConfiguration.getEvent()).thenReturn("event");
            when(feesConfiguration.getService()).thenReturn("general");
            when(feesConfiguration.getChannel()).thenReturn("default");
            when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
            when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
            when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn("GeneralAppWithoutNotice");

            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(WithoutNotice)
            )).thenReturn(FEE_POUNDS_108);

            List<GeneralApplicationTypes> allTypes =
                Stream.of(GeneralApplicationTypes.values()).collect(Collectors.toList());
            allTypes.removeAll(GeneralAppFeesService.VARY_TYPES);
            allTypes.removeAll(GeneralAppFeesService.SET_ASIDE);
            allTypes.removeAll(GeneralAppFeesService.ADJOURN_TYPES);
            allTypes.removeAll(GeneralAppFeesService.SD_CONSENT_TYPES);
            allTypes.removeAll(GeneralAppFeesService.CONFIRM_YOU_PAID_CCJ_DEBT);
            //single
            for (GeneralApplicationTypes generalApplicationType : allTypes) {
                CaseData caseData = getFeeCase(
                    List.of(generalApplicationType), YesOrNo.NO, YesOrNo.NO, null);
                Fee feeDto = feesService.getFeeForGA(caseData);
                assertThat(feeDto).isEqualTo(FEE_PENCE_108);
            }
            //mix
            CaseData caseData = getFeeCase(
                allTypes, YesOrNo.NO, YesOrNo.NO, null);
            Fee feeDto = feesService.getFeeForGA(caseData);
            assertThat(feeDto).isEqualTo(FEE_PENCE_108);
        }

        @Test
        void adjourn_should_pay_default_or_free_fee() {
            when(feesConfiguration.getEvent()).thenReturn("event");
            when(feesConfiguration.getService()).thenReturn("general");
            when(feesConfiguration.getChannel()).thenReturn("default");
            when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
            when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
            when(feesConfiguration.getWithNoticeKeyword()).thenReturn("GAOnNotice");
            when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn("GeneralAppWithoutNotice");

            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(WithoutNotice)
            )).thenReturn(FEE_POUNDS_108);
            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(GAOnNotice)
            )).thenReturn(FEE_POUNDS_275);

            CaseData caseDataWithin14DaysWithNotice = getFeeCase(
                List.of(GeneralApplicationTypes.ADJOURN_HEARING),
                YesOrNo.NO, YesOrNo.YES, LocalDate.now().plusDays(1)
            );
            assertThat(feesService.getFeeForGA(caseDataWithin14DaysWithNotice))
                .isEqualTo(FEE_PENCE_275);
            CaseData caseDataWithin14DaysWithoutNotice = getFeeCase(
                List.of(GeneralApplicationTypes.ADJOURN_HEARING),
                YesOrNo.NO, YesOrNo.NO, LocalDate.now().plusDays(1)
            );
            assertThat(feesService.getFeeForGA(caseDataWithin14DaysWithoutNotice))
                .isEqualTo(FEE_PENCE_108);
            CaseData caseDataOutside14Days = getFeeCase(
                List.of(GeneralApplicationTypes.ADJOURN_HEARING),
                YesOrNo.YES, YesOrNo.NO, LocalDate.now().plusDays(15)
            );
            assertThat(feesService.getFeeForGA(caseDataOutside14Days))
                .isEqualTo(FEE_PENCE_0);
        }

        @Test
        void vary_types_should_be_14() {
            when(feesConfiguration.getChannel()).thenReturn("default");
            when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
            when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
            when(feesConfiguration.getAppnToVaryOrSuspend()).thenReturn("AppnToVaryOrSuspend");

            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(AppnToVaryOrSuspend)
            )).thenReturn(FEE_POUNDS_14);

            for (GeneralApplicationTypes type : GeneralAppFeesService.VARY_TYPES) {
                CaseData caseDataWithNotice = getFeeCase(
                    List.of(type),
                    YesOrNo.YES, YesOrNo.YES, null
                );
                Fee feeDto = feesService.getFeeForGA(caseDataWithNotice);
                assertThat(feeDto).isEqualTo(FEE_PENCE_14);
                CaseData caseDataWithoutNotice = getFeeCase(
                    List.of(type),
                    YesOrNo.NO, YesOrNo.NO, null
                );
                feeDto = feesService.getFeeForGA(caseDataWithoutNotice);
                assertThat(feeDto).isEqualTo(FEE_PENCE_14);
            }
        }

        @Test
        void settle_should_be_108() {
            when(feesConfiguration.getEvent()).thenReturn("event");
            when(feesConfiguration.getService()).thenReturn("general");
            when(feesConfiguration.getChannel()).thenReturn("default");
            when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
            when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
            when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn("GeneralAppWithoutNotice");

            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(WithoutNotice)
            )).thenReturn(FEE_POUNDS_108);

            CaseData caseDataWithNotice = getFeeCase(
                List.of(GeneralApplicationTypes.SETTLE_BY_CONSENT),
                YesOrNo.YES, YesOrNo.YES, null
            );
            Fee feeDto = feesService.getFeeForGA(caseDataWithNotice);
            assertThat(feeDto).isEqualTo(FEE_PENCE_108);
        }

        @Test
        void setAside_should_be_275() {
            when(feesConfiguration.getEvent()).thenReturn("event");
            when(feesConfiguration.getService()).thenReturn("general");
            when(feesConfiguration.getChannel()).thenReturn("default");
            when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
            when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
            when(feesConfiguration.getWithNoticeKeyword()).thenReturn("GAOnNotice");
            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(GAOnNotice)
            )).thenReturn(FEE_POUNDS_275);

            CaseData caseDataWithNotice = getFeeCase(
                List.of(GeneralApplicationTypes.SET_ASIDE_JUDGEMENT),
                YesOrNo.YES, YesOrNo.YES, null
            );
            Fee feeDto = feesService.getFeeForGA(caseDataWithNotice);
            assertThat(feeDto).isEqualTo(FEE_PENCE_275);
            CaseData caseDataWithoutNotice = getFeeCase(
                List.of(GeneralApplicationTypes.SET_ASIDE_JUDGEMENT),
                YesOrNo.NO, YesOrNo.NO, null
            );
            feeDto = feesService.getFeeForGA(caseDataWithoutNotice);
            assertThat(feeDto).isEqualTo(FEE_PENCE_275);
        }

        @Test
        void mix_default_adjourn_should_not_free() {
            when(feesConfiguration.getEvent()).thenReturn("event");
            when(feesConfiguration.getService()).thenReturn("general");
            when(feesConfiguration.getChannel()).thenReturn("default");
            when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
            when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
            when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn("GeneralAppWithoutNotice");

            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(WithoutNotice)
            )).thenReturn(FEE_POUNDS_108);

            List<GeneralApplicationTypes> randomList = getRandomDefaultTypes();
            randomList.add(GeneralApplicationTypes.ADJOURN_HEARING);
            CaseData caseDataOutside14Days = getFeeCase(
                randomList,
                YesOrNo.YES, YesOrNo.YES, LocalDate.now().plusDays(15)
            );
            assertThat(feesService.getFeeForGA(caseDataOutside14Days))
                .isEqualTo(FEE_PENCE_108);
        }

        @Test
        void mix_default_set_aside_should_be_108() {
            when(feesConfiguration.getEvent()).thenReturn("event");
            when(feesConfiguration.getService()).thenReturn("general");
            when(feesConfiguration.getChannel()).thenReturn("default");
            when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
            when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
            when(feesConfiguration.getWithNoticeKeyword()).thenReturn("GAOnNotice");
            when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn("GeneralAppWithoutNotice");

            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(WithoutNotice)
            )).thenReturn(FEE_POUNDS_108);
            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(GAOnNotice)
            )).thenReturn(FEE_POUNDS_275);

            List<GeneralApplicationTypes> randomList = getRandomDefaultTypes();
            randomList.add(GeneralApplicationTypes.SET_ASIDE_JUDGEMENT);
            CaseData caseDataOutside14Days = getFeeCase(
                randomList,
                YesOrNo.YES, YesOrNo.YES, LocalDate.now().plusDays(15)
            );
            assertThat(feesService.getFeeForGA(caseDataOutside14Days))
                .isEqualTo(FEE_PENCE_108);
        }

        @Test
        void mix_default_vary_should_be_14() {
            when(feesConfiguration.getEvent()).thenReturn("event");
            when(feesConfiguration.getService()).thenReturn("general");
            when(feesConfiguration.getChannel()).thenReturn("default");
            when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
            when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
            when(feesConfiguration.getAppnToVaryOrSuspend()).thenReturn("AppnToVaryOrSuspend");
            when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn("GeneralAppWithoutNotice");

            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(WithoutNotice)
            )).thenReturn(FEE_POUNDS_108);

            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(AppnToVaryOrSuspend)
            )).thenReturn(FEE_POUNDS_14);

            List<GeneralApplicationTypes> randomList = getRandomDefaultTypes();
            randomList.add(GeneralApplicationTypes.VARY_PAYMENT_TERMS_OF_JUDGMENT);

            CaseData caseDataOutside14Days = getFeeCase(
                randomList,
                YesOrNo.YES, YesOrNo.YES, LocalDate.now().plusDays(15)
            );
            assertThat(feesService.getFeeForGA(caseDataOutside14Days))
                .isEqualTo(FEE_PENCE_14);
            randomList.remove(randomList.size() - 1);
            randomList.add(GeneralApplicationTypes.VARY_ORDER);
            caseDataOutside14Days = getFeeCase(
                randomList,
                YesOrNo.YES, YesOrNo.YES, LocalDate.now().plusDays(15)
            );
            assertThat(feesService.getFeeForGA(caseDataOutside14Days))
                .isEqualTo(FEE_PENCE_14);
            randomList.remove(randomList.size() - 1);
        }

        @Test
        void mix_default_vary_set_aside_should_be_14() {
            when(feesConfiguration.getEvent()).thenReturn("event");
            when(feesConfiguration.getService()).thenReturn("general");
            when(feesConfiguration.getChannel()).thenReturn("default");
            when(feesConfiguration.getJurisdiction1()).thenReturn("civil");
            when(feesConfiguration.getJurisdiction2()).thenReturn("civil");
            when(feesConfiguration.getWithNoticeKeyword()).thenReturn("GAOnNotice");
            when(feesConfiguration.getAppnToVaryOrSuspend()).thenReturn("AppnToVaryOrSuspend");
            when(feesConfiguration.getConsentedOrWithoutNoticeKeyword()).thenReturn("GeneralAppWithoutNotice");

            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(WithoutNotice)
            )).thenReturn(FEE_POUNDS_108);
            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(GAOnNotice)
            )).thenReturn(FEE_POUNDS_275);
            when(feesApiClient.lookupFee(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(AppnToVaryOrSuspend)
            )).thenReturn(FEE_POUNDS_14);

            List<GeneralApplicationTypes> randomList = getRandomDefaultTypes();
            randomList.add(GeneralApplicationTypes.VARY_ORDER);
            randomList.add(GeneralApplicationTypes.SET_ASIDE_JUDGEMENT);
            CaseData caseDataOutside14Days = getFeeCase(
                randomList,
                YesOrNo.YES, YesOrNo.YES, LocalDate.now().plusDays(15)
            );
            assertThat(feesService.getFeeForGA(caseDataOutside14Days))
                .isEqualTo(FEE_PENCE_14);
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
            Fee feeDto = feesService.getFeeForGA(caseData);
            assertThat(feeDto).isEqualTo(FEE_PENCE_15);
        }

        private List<GeneralApplicationTypes> getRandomDefaultTypes() {
            List<GeneralApplicationTypes> allTypes =
                Stream.of(GeneralApplicationTypes.values()).collect(Collectors.toList());
            allTypes.removeAll(GeneralAppFeesService.VARY_TYPES);
            allTypes.removeAll(GeneralAppFeesService.SET_ASIDE);
            allTypes.removeAll(GeneralAppFeesService.ADJOURN_TYPES);
            allTypes.removeAll(GeneralAppFeesService.SD_CONSENT_TYPES);
            allTypes.removeAll(GeneralAppFeesService.CONFIRM_YOU_PAID_CCJ_DEBT);
            Collections.shuffle(allTypes);
            Random rand = new Random();
            int min = 1;
            int max = allTypes.size();
            return allTypes.subList(0, rand.nextInt(min, max));
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
    }
}
