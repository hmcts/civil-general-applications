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

    public PaymentsConfiguration(@Value("${payments.api.site_id}") String siteId,
                                 @Value("${payments.api.service}") String service,
                                 @Value("${payments.api.callback_url}") String payApiCallBackUrl) {
        this.siteId = siteId;
        this.service = service;
        this.payApiCallBackUrl = payApiCallBackUrl;
    }
}
