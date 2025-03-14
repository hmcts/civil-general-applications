package uk.gov.hmcts.reform.civil.service.docmosis;

import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.flowstate.FlowFlag;

import java.io.IOException;

public interface TemplateDataGenerator<T> {

    default T getTemplateData(CaseData caseData) throws IOException {
        return null;
    }

    default T getTemplateData(CaseData caseData, String authorisation) throws IOException {
        return null;
    }

    default T getTemplateData(CaseData civilCaseData, CaseData caseData, String authorisation, FlowFlag userType) throws IOException {
        return null;
    }
}
