package uk.gov.hmcts.reform.civil.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BulkPrintService {

    private static final Logger log = LoggerFactory.getLogger(BulkPrintService.class);
    public static final String XEROX_TYPE_PARAMETER = "CMC001";
    protected static final String ADDITIONAL_DATA_LETTER_TYPE_KEY = "letterType";
    protected static final String ADDITIONAL_DATA_CASE_IDENTIFIER_KEY = "caseIdentifier";
    protected static final String ADDITIONAL_DATA_CASE_REFERENCE_NUMBER_KEY = "caseReferenceNumber";
    protected static final String RECIPIENTS = "recipients";
    private final SendLetterApi sendLetterApi;
    private final AuthTokenGenerator authTokenGenerator;

    @Retryable(
        value = {RuntimeException.class},
        backoff = @Backoff(
            delay = 200L
        )
    )
    public SendLetterResponse printLetter(byte[] letterContent, String claimId, String claimReference, String letterType, List<String> personList) {
        String authorisation = this.authTokenGenerator.generate();
        LetterWithPdfsRequest letter = this.generateLetter(this.additionalInformation(claimId, claimReference, letterType, personList), letterContent);
        return this.sendLetterApi.sendLetter(authorisation, letter);
    }

    private LetterWithPdfsRequest generateLetter(Map<String, Object> letterParams, byte[] letterContent) {
        String templateLetter = Base64.getEncoder().encodeToString(letterContent);
        return new LetterWithPdfsRequest(List.of(templateLetter), XEROX_TYPE_PARAMETER, letterParams);
    }

    private Map<String, Object> additionalInformation(String claimId, String claimReference, String letterType, List<String> personList) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put(ADDITIONAL_DATA_LETTER_TYPE_KEY, letterType);
        additionalData.put(ADDITIONAL_DATA_CASE_IDENTIFIER_KEY, claimId);
        additionalData.put(ADDITIONAL_DATA_CASE_REFERENCE_NUMBER_KEY, claimReference);
        additionalData.put(RECIPIENTS, personList);
        return additionalData;
    }

    public BulkPrintService(final SendLetterApi sendLetterApi, final AuthTokenGenerator authTokenGenerator) {
        this.sendLetterApi = sendLetterApi;
        this.authTokenGenerator = authTokenGenerator;
    }
}
