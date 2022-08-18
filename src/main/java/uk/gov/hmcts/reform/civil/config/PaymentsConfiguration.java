package uk.gov.hmcts.reform.civil.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class PaymentsConfiguration {

    private final String siteId;
    private final String service;
    private String payApiCallBackUrl;
    private final String specSiteId;

    public PaymentsConfiguration(@Value("${payments.api.site_id}") String siteId,
                                 @Value("${payments.api.service}") String service,
                                 @Value("${payments.api.callback-url}") String payApiCallBackUrl,
                                 @Value("${payments.api.spec_site_id}") String specSiteId) {
        this.siteId = siteId;
        this.service = service;
        this.payApiCallBackUrl = payApiCallBackUrl;
        this.specSiteId = specSiteId;
    }
}
