package uk.gov.hmcts.reform.civil.service.docmosis.finalorder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;

import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class AssistedOrderFormGeneratorTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(" d MMMM yyyy");
    private static final String FILE_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String LINE_BREAKER = "\n\n";
    private static final String ORDER_MADE_ON_NONE_TEXT = "This order was not made on the court’s own initiative"
        +" or without notice.";
    private static final String CLAIMANT_DEFENDANT_BOTH_ATTENDED_TEXT = "The Judge heard from %s and %s.";
    private static final String CLAIMANT_OR_DEFENDANT_ATTENDED_TEXT = "The Judge heard from %s.";

    private static final String CLAIMANT_NOT_ATTENDED_TEXT = "The claimant did not attend the trial";
    private static final String DEFENDANT_NOT_ATTENDED_TEXT = "The defendant did not attend the trial";
    private static final String JUDGE_SATISFIED_TO_PROCEED_TEXT = ", but the Judge was satisfied that they had received"
        + " notice of the trial and it was reasonable to proceed in their absence.";
    private static final String JUDGE_SATISFIED_NOTICE_OF_TRIAL_TEXT = " and whilst the Judge was satisfied that they"
        + " had received notice of the trial it was not reasonable to proceed in their absence.";
    private static final String JUDGE_NOT_SATISFIED_NOTICE_OF_TRIAL_TEXT = ", but the Judge was not satisfied that they"
        + " had received notice of the hearing and it was not reasonable to proceed in their absence.";

    private static final String JUDGE_HEARD_FROM_TEXT = "The Judge heard other representation: %s";
    private static final String JUDGE_CONSIDERED_PAPERS_TEXT = "The judge considered the papers.";
    private static final String RECITAL_RECORDED_TEXT = "It is recorded that %s.";
    private static final String COST_IN_CASE_TEXT = "Costs in the case have been ordered.";
    private static final String NO_ORDER_COST_TEXT = "No order as to costs has been made.";
    private static final String COSTS_RESERVED_TEXT = "Costs reserved:%s.";
    private static final String COST_AMOUNT_TEXT = "Amount: %s ";
    private static final String COST_PAID_BY_TEXT =  "To be paid by: %s.";
    private static final String COST_PARTY_HAS_BENEFIT_TEXT = "The paying party has the benefit of cost protection"
        + " under section 26 Sentencing and Punishment Offenders Act 2012. The amount of the costs pay shall"
        + " be determined on an application by the receiving party under Legal Aid (Costs) Regulations 2013."
        + " Any objection by the paying party claimed shall be dealt with on that occasion.";
    private static final String COST_PARTY_NO_BENEFIT_TEXT =  "The paying party does not have cost protection.";
    private static final String COST_BESPOKE_TEXT = "Bespoke costs orders: %s ";

    private static final String FURTHER_HEARING_TAKE_PLACE_AFTER_TEXT = "A further hearing will take place after: %s ";
    private static final String FURTHER_HEARING_TAKE_PLACE_BEFORE_TEXT = "It will take place before: %s";
    private static final String FURTHER_HEARING_LENGTH_TEXT = "The length of new hearing will be: %s";
    private static final String FURTHER_HEARING_LENGTH_OTHER = " %s/ %s/ %s";
    private static final String FURTHER_HEARING_ALTERNATIVE_HEARING_TEXT = "Alternative hearing location: %s";
    private static final String FURTHER_HEARING_METHOD_HEARING_TEXT = "Method of hearing: %s";
    private static final String PERMISSION_TO_APPEAL_TEXT = "The application for permission to appeal "
        + "for the %s is %s.";
    private static final String permissionToAppealReasonsText = "Reasons: %s ";

    @MockBean
    private UnsecuredDocumentManagementService documentManagementService;

    @MockBean
    private DocumentGeneratorService documentGeneratorService;

    @MockBean
    private CoreCaseDataService coreCaseDataService;
    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @Autowired
    private FreeFormOrderGenerator generator;
    
    @Test
    void generate() {
    }

    @Test
    void getTemplateData() {
    }

    @Test
    void getCostsTextValue() {
    }

    @Test
    void getFurtherHearingText() {
    }

    @Test
    void getRecitalRecordedText() {
    }

    @Test
    void generalJudgeHeardFromText() {
    }

    @Test
    void getOrderMadeOnText() {
    }

    @Test
    void getOrderMadeDate() {
    }

    @Test
    void getReasonText() {
    }

    @Test
    void getFileName() {
    }

    @Test
    void getReference() {
    }

    @Test
    void getCostText() {
    }

    @Test
    void getJudgeSatisfiedText() {
    }

    @Test
    void getIsProtectionDateText() {
    }

    @Test
    void getTemplate() {
    }
}
