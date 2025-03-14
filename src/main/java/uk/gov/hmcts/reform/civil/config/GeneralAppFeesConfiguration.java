package uk.gov.hmcts.reform.civil.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class GeneralAppFeesConfiguration {

    private final String url;
    private final String endpoint;
    private final String service;
    private final String jurisdiction1;
    private final String jurisdiction2;
    private final String channel;
    private final String event;
    private final String withNoticeKeyword;
    private final String consentedOrWithoutNoticeKeyword;
    private final String applicationUncloakAdditionalFee;
    private final String appnToVaryOrSuspend;
    private final String certificateOfSatisfaction;

    public GeneralAppFeesConfiguration(
            @Value("${fees.api.url}") String url,
            @Value("${genApp.fee.endpoint}") String endpoint,
            @Value("${genApp.fee.service}") String service,
            @Value("${genApp.fee.jurisdiction1}") String jurisdiction1,
            @Value("${genApp.fee.jurisdiction2}") String jurisdiction2,
            @Value("${genApp.fee.channel}") String channel,
            @Value("${genApp.fee.event}") String event,
            @Value("${genApp.fee.keywords.withNotice}") String withNoticeKeyword,
            @Value("${genApp.fee.keywords.consentedOrWithoutNotice}") String consentedOrWithoutNoticeKeyword,
            @Value("${genApp.fee.keywords.uncloakFee}") String applicationUncloakAdditionalFee,
            @Value("${genApp.fee.keywords.appnToVaryOrSuspend}") String appnToVaryOrSuspend,
            @Value("${genApp.fee.keywords.certificateOfSatisfaction}") String certificateOfSatisfaction) {
        this.url = url;
        this.endpoint = endpoint;
        this.service = service;
        this.jurisdiction1 = jurisdiction1;
        this.jurisdiction2 = jurisdiction2;
        this.channel = channel;
        this.event = event;
        this.withNoticeKeyword = withNoticeKeyword;
        this.consentedOrWithoutNoticeKeyword = consentedOrWithoutNoticeKeyword;
        this.applicationUncloakAdditionalFee = applicationUncloakAdditionalFee;
        this.appnToVaryOrSuspend = appnToVaryOrSuspend;
        this.certificateOfSatisfaction = certificateOfSatisfaction;
    }

}
