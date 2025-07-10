package uk.gov.hmcts.reform.civil.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class NotificationsSignatureConfiguration {

    private final String hmctsSignature;
    private final String phoneContact;
    private final String openingHours;
    private final String specUnspecContact;
    private final String specContact;
    private final String welshContact;
    private final String welshHmctsSignature;
    private final String welshPhoneContact;
    private final String welshOpeningHours;

    public NotificationsSignatureConfiguration(@Value("${notifications.hmctsSignature}") String hmctsSignature,
                                               @Value("${notifications.phoneContact}") String phoneContact,
                                               @Value("${notifications.openingHours}") String openingHours,
                                               @Value("${notifications.specUnspecContact}") String specUnspecContact,
                                               @Value("${notifications.specContact}") String specContact,
                                               @Value("${notifications.welshContact}") String welshContact,
                                               @Value("${notifications.welshHmctsSignature}") String welshHmctsSignature,
                                               @Value("${notifications.welshPhoneContact}") String welshPhoneContact,
                                               @Value("${notifications.welshOpeningHours}") String welshOpeningHours) {

        this.hmctsSignature = hmctsSignature;
        this.phoneContact = phoneContact;
        this.openingHours = openingHours;
        this.specUnspecContact = specUnspecContact;
        this.specContact = specContact;
        this.welshContact = welshContact;
        this.welshHmctsSignature = welshHmctsSignature;
        this.welshPhoneContact = welshPhoneContact;
        this.welshOpeningHours = welshOpeningHours;
    }
}
