package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.civil.client.FeesApiClient;
import uk.gov.hmcts.reform.civil.config.GeneralAppFeesConfiguration;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralAppFeesService {

    private static final BigDecimal PENCE_PER_POUND = BigDecimal.valueOf(100);
    public static final int FREE_GA_DAYS = 14;
    private final FeesApiClient feesApiClient;
    private final GeneralAppFeesConfiguration feesConfiguration;
    public static final String FREE_REF = "FREE";
    private static final Fee FREE_FEE = Fee.builder()
        .calculatedAmountInPence(BigDecimal.ZERO).code(FREE_REF).version("1").build();
    protected static final List<GeneralApplicationTypes> VARY_TYPES
        = Arrays.asList(
        GeneralApplicationTypes.VARY_PAYMENT_TERMS_OF_JUDGMENT
    );
    protected static final List<GeneralApplicationTypes> SET_ASIDE
        = List.of(GeneralApplicationTypes.SET_ASIDE_JUDGEMENT);
    protected static final List<GeneralApplicationTypes> ADJOURN_TYPES
        = List.of(GeneralApplicationTypes.ADJOURN_HEARING);
    protected static final List<GeneralApplicationTypes> SD_CONSENT_TYPES
        = List.of(GeneralApplicationTypes.SETTLE_BY_CONSENT);
    protected static final List<GeneralApplicationTypes> CONFIRM_YOU_PAID_CCJ_DEBT
        = List.of(GeneralApplicationTypes.CONFIRM_CCJ_DEBT_PAID);

    public Fee getFeeForGA(CaseData caseData) {
        Fee result = Fee.builder().calculatedAmountInPence(BigDecimal.valueOf(Integer.MAX_VALUE)).build();
        int typeSize = caseData.getGeneralAppType().getTypes().size();
        if (CollectionUtils.containsAny(caseData.getGeneralAppType().getTypes(), VARY_TYPES)) {
            //only minus 1 as VARY_PAYMENT_TERMS_OF_JUDGMENT can't be multi selected
            typeSize--;
            result = getFeeForGA(feesConfiguration.getAppnToVaryOrSuspend(), "miscellaneous", "other");
        }
        if (typeSize > 0
            && CollectionUtils.containsAny(caseData.getGeneralAppType().getTypes(), SD_CONSENT_TYPES)) {
            typeSize--;
            Fee sdConsentFeeForGA = getFeeForGA(feesConfiguration.getConsentedOrWithoutNoticeKeyword(), null, null);
            if (sdConsentFeeForGA.getCalculatedAmountInPence()
                .compareTo(result.getCalculatedAmountInPence()) < 0) {
                result = sdConsentFeeForGA;
            }
        }
        if (typeSize > 0
            && CollectionUtils.containsAny(caseData.getGeneralAppType().getTypes(), SET_ASIDE)
            && caseData.getGeneralAppRespondentAgreement() != null
            && NO.equals(caseData.getGeneralAppRespondentAgreement().getHasAgreed())) {
            String feeKeyword;
            if (caseData.getGeneralAppInformOtherParty() != null
                && NO.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice())) {
                feeKeyword = feesConfiguration.getConsentedOrWithoutNoticeKeyword();
            } else {
                feeKeyword = feesConfiguration.getWithNoticeKeyword();
            }
            typeSize--;
            Fee setAsideFeeForGA = getFeeForGA(feeKeyword, null, null);
            if (setAsideFeeForGA.getCalculatedAmountInPence()
                .compareTo(result.getCalculatedAmountInPence()) < 0) {
                result = setAsideFeeForGA;
            }
        }
        if (isUpdateCoScGATypeSize(typeSize, caseData.getGeneralAppType().getTypes())) {
            typeSize--;
            Fee certOfSatisfactionOrCancel = getFeeForGA(feesConfiguration.getCertificateOfSatisfaction(), "miscellaneous", "other");
            result = getCoScFeeResult(result, certOfSatisfactionOrCancel);
        }
        if (typeSize > 0) {
            Fee defaultFee = getDefaultFee(caseData);
            if (defaultFee.getCalculatedAmountInPence()
                .compareTo(result.getCalculatedAmountInPence()) < 0) {
                result = defaultFee;
            }
        }
        return result;
    }

    public Fee getFeeForGA(String keyword, String event, String service) {
        if (Objects.isNull(event)) {
            event = feesConfiguration.getEvent();
        }
        if (Objects.isNull(service)) {
            service = feesConfiguration.getService();
        }

        FeeLookupResponseDto feeLookupResponseDto;
        try {
            feeLookupResponseDto = feesApiClient.lookupFee(
                service,
                feesConfiguration.getJurisdiction1(),
                feesConfiguration.getJurisdiction2(),
                feesConfiguration.getChannel(),
                event,
                keyword
            );
        } catch (Exception e) {
            log.error("Fee Service Lookup Failed - " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        if (feeLookupResponseDto == null || feeLookupResponseDto.getFeeAmount() == null) {
            log.error("No Fees returned");
            throw new RuntimeException("No Fees returned by fee-service while creating General Application");
        }
        return buildFeeDto(feeLookupResponseDto);
    }

    private Fee getDefaultFee(CaseData caseData) {
        if (isFreeApplication(caseData)) {
            return FREE_FEE;
        } else {
            return getFeeForGA(getFeeRegisterKeyword(caseData), null, null);
        }
    }

    protected String getFeeRegisterKeyword(CaseData caseData) {
        boolean isNotified = caseData.getGeneralAppRespondentAgreement() != null
            && NO.equals(caseData.getGeneralAppRespondentAgreement().getHasAgreed())
            && caseData.getGeneralAppInformOtherParty() != null
            && YES.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice());
        return isNotified
            ? feesConfiguration.getWithNoticeKeyword()
            : feesConfiguration.getConsentedOrWithoutNoticeKeyword();
    }

    public boolean isFreeApplication(final CaseData caseData) {
        if (caseData.getGeneralAppType().getTypes().size() == 1
            && caseData.getGeneralAppType().getTypes()
            .contains(GeneralApplicationTypes.ADJOURN_HEARING)
            && caseData.getGeneralAppRespondentAgreement() != null
            && YES.equals(caseData.getGeneralAppRespondentAgreement().getHasAgreed())
            && caseData.getGeneralAppHearingDate() != null
            && caseData.getGeneralAppHearingDate().getHearingScheduledDate() != null) {
            return caseData.getGeneralAppHearingDate().getHearingScheduledDate()
                .isAfter(LocalDate.now().plusDays(FREE_GA_DAYS));
        }
        return false;
    }

    public boolean isFreeGa(GeneralApplication application) {
        if (application.getGeneralAppType().getTypes().size() == 1
            && application.getGeneralAppType().getTypes()
            .contains(GeneralApplicationTypes.ADJOURN_HEARING)
            && application.getGeneralAppRespondentAgreement() != null
            && YES.equals(application.getGeneralAppRespondentAgreement().getHasAgreed())
            && application.getGeneralAppHearingDate() != null
            && application.getGeneralAppHearingDate().getHearingScheduledDate() != null) {
            return application.getGeneralAppHearingDate().getHearingScheduledDate()
                .isAfter(LocalDate.now().plusDays(GeneralAppFeesService.FREE_GA_DAYS));
        }
        return false;
    }

    private Fee buildFeeDto(FeeLookupResponseDto feeLookupResponseDto) {
        BigDecimal calculatedAmount = feeLookupResponseDto.getFeeAmount()
            .multiply(PENCE_PER_POUND)
            .setScale(0, RoundingMode.UNNECESSARY);

        return Fee.builder()
            .calculatedAmountInPence(calculatedAmount)
            .code(feeLookupResponseDto.getCode())
            .version(feeLookupResponseDto.getVersion().toString())
            .build();
    }

    private boolean isUpdateCoScGATypeSize(int typeSize, List<GeneralApplicationTypes> types) {
        return typeSize > 0 && CollectionUtils.containsAny(types, CONFIRM_YOU_PAID_CCJ_DEBT);
    }

    private Fee getCoScFeeResult(Fee existingResult, Fee certOfSatisfactionOrCancel) {
        if (certOfSatisfactionOrCancel.getCalculatedAmountInPence().compareTo(existingResult.getCalculatedAmountInPence()) < 0) {
            return certOfSatisfactionOrCancel;
        }
        return existingResult;
    }
}
