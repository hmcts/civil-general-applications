package uk.gov.hmcts.reform.civil.service.docmosis.finalorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.*;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.AssistedOrderForm;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderCost;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.civil.enums.dq.AssistedCostTypesList.*;
import static uk.gov.hmcts.reform.civil.enums.dq.ClaimantRepresentationType.CLAIMANT_NOT_ATTENDING;
import static uk.gov.hmcts.reform.civil.enums.dq.FinalOrderConsideredToggle.CONSIDERED;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.ASSISTED_ORDER_FORM;

@Service
@RequiredArgsConstructor
public class AssistedOrderFormGenerator implements TemplateDataGenerator<AssistedOrderForm> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final CoreCaseDataService coreCaseDataService;

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
    private static final String JUDGE_SATISFIED_NOTICE_OF_TRIAL_TEXT = " and whilst the Judge was satisfied that they had"
        + " received notice of the trial it was not reasonable to proceed in their absence.";
    private static final String JUDGE_NOT_SATISFIED_NOTICE_OF_TRIAL_TEXT = ", but the Judge was not satisfied that they had"
        + " received notice of the hearing and it was not reasonable to proceed in their absence.";

    private static final String judgeHeardFromText = "The Judge heard other representation: %s";
    private static final String judgeConsiderPapersText = "The judge considered the papers.";
    private static final String recitalRecordedText = "It is recorded that %s.";
    private static final String costInCaseText = "Costs in the case have been ordered.";
    private static final String noOrderToCost = "No order as to costs has been made.";
    private static final String costsReserved = "Costs reserved:%s.";
    private static final String costAmount = "Amount: %s ";
    private static final String costToBePaidBy =  "To be paid by: %s.";
    private static final String costPartyHasBenefit = "The paying party has the benefit of cost protection under section 26 Sentencing and Punishment Offenders Act 2012. The amount of the costs pay shall be determined on an application by the receiving party under Legal Aid (Costs) Regulations 2013. Any objection by the paying party claimed shall be dealt with on that occasion.";
    private static final String costPartyHasNoBenefit =  "The paying party does not have cost protection.";
    private static final String costBespokeText = "Bespoke costs orders: %s ";

    private static final String furtherHearingTakePlaceAfterText = "A further hearing will take place after: %s ";
    private static final String furtherHearingTakePlaceBeforeText = "It will take place before: %s";
    private static final String furtherHearingLengthText = "The length of new hearing will be: %s";
    private static final String furtherHearingLengthOther = " %s/ %s/ %s";
    private static final String furtherHearingAlternativeHearingText = "Alternative hearing location: %s";
    private static final String furtherHearingMethodHearingText = "Method of hearing: %s";
    private static final String permissionToAppealText = "The application for permission to appeal "
                                                        + "for the %s is %s.";
    private static final String permissionToAppealReasonsText = "Reasons: %s ";


    public CaseDocument generate(CaseData caseData, String authorisation) {

        AssistedOrderForm templateData = getTemplateData(caseData);
        DocmosisTemplates template = getTemplate();
        DocmosisDocument document = documentGeneratorService.generateDocmosisDocument(templateData, template);

        return documentManagementService.uploadDocument(
                authorisation,
                new PDF(
                        getFileName(template),
                        document.getBytes(),
                        DocumentType.GENERAL_ORDER
                )
        );
    }

    @Override
    public AssistedOrderForm getTemplateData(CaseData caseData) {
        CaseDetails parentCase = coreCaseDataService
                .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference()));

        return AssistedOrderForm.builder()
                .caseNumber(getCaseNumberFormatted(caseData))
                .caseName(caseData.getCaseNameHmctsInternal())
                .receivedDate(getDateFormatted(LocalDate.now()))
                .claimantReference(getReference(parentCase, "applicantSolicitor1Reference"))
                .defendantReference(getReference(parentCase, "respondentSolicitor1Reference"))
                .isOrderMade(caseData.getAssistedOrderMadeSelection().equals(YesOrNo.YES))
                .orderMadeDate(getOrderMadeDate(caseData))
                .judgeHeardFromText(generalJudgeHeardFromText(caseData))
                .recitalRecordedText(getRecitalRecordedText(caseData))
                .orderedText(caseData.getAssistedOrderOrderedThatText())
                .costsText(getCostsTextValue(caseData))
                .furtherHearingText(getFurtherHearingText(caseData))
                .permissionToAppealText(getPermissionToAppealText(caseData))
                .orderMadeOnText(getOrderMadeOnText(caseData))
                .reasonText(getReasonText(caseData))
                .build();
    }

    protected String getCostsTextValue(CaseData caseData) {
        StringBuilder costsTextBuilder = new StringBuilder();
        if(isNull(caseData.getAssistedCostTypes())){
            return null;
        }else {
            switch (caseData.getAssistedCostTypes()){
                case COSTS_IN_CASE: {
                    costsTextBuilder.append(costInCaseText);
                }
                break;
                case NO_ORDER_TO_COST: {
                    costsTextBuilder.append(noOrderToCost);
                }
                break;
                case COSTS_RESERVED: {
                    if (nonNull(caseData.getCostReservedDetails())
                        && nonNull(caseData.getCostReservedDetails().getDetailText()) ) {
                        costsTextBuilder.append(String.format(
                            costsReserved,
                            caseData.getCostReservedDetails().getDetailText()
                        ));
                    }
                }
                break;
                case DEFENDANT_COST_STANDARD_BASE: {
                    costsTextBuilder.append(DEFENDANT_COST_STANDARD_BASE.getDisplayedValue());
                    costsTextBuilder.append(". ");
                    if(nonNull(caseData.getDefendantCostStandardBase())){
                        costsTextBuilder.append(LINE_BREAKER);
                        costsTextBuilder.append(getCostText(caseData.getDefendantCostStandardBase()));
                        costsTextBuilder.append(getIsProtectionDateText(caseData.getDefendantCostStandardBase()));
                    }
                }
                break;

                case CLAIMANT_COST_STANDARD_BASE: {
                    costsTextBuilder.append(CLAIMANT_COST_STANDARD_BASE.getDisplayedValue());
                    costsTextBuilder.append(". ");
                    if(nonNull(caseData.getClaimantCostStandardBase())){
                        costsTextBuilder.append(LINE_BREAKER);
                        costsTextBuilder.append(getCostText(caseData.getClaimantCostStandardBase()));
                        costsTextBuilder.append(getIsProtectionDateText(caseData.getClaimantCostStandardBase()));
                    }
                }
                break;

                case DEFENDANT_COST_SUMMARILY_BASE: {
                    costsTextBuilder.append(DEFENDANT_COST_SUMMARILY_BASE.getDisplayedValue());
                    costsTextBuilder.append(" ");
                    costsTextBuilder.append(caseData.getDefendantCostSummarilyBase().formatCaseAmountToPounds());

                    if(nonNull(caseData.getDefendantCostSummarilyBase())){
                        costsTextBuilder.append(getIsProtectionDateText(caseData.getDefendantCostSummarilyBase()));
                    }
                }
                break;

                case CLAIMANT_COST_SUMMARILY_BASE: {
                    costsTextBuilder.append(CLAIMANT_COST_SUMMARILY_BASE.getDisplayedValue());
                    costsTextBuilder.append(" ");
                    costsTextBuilder.append(caseData.getClaimantCostSummarilyBase().formatCaseAmountToPounds());

                    if(nonNull(caseData.getClaimantCostSummarilyBase())){
                        costsTextBuilder.append(LINE_BREAKER);
                        costsTextBuilder.append(getIsProtectionDateText(caseData.getClaimantCostSummarilyBase()));
                    }
                }
                break;
                case BESPOKE_COSTS_ORDER: {
                    if (nonNull(caseData.getBespokeCostDetails())) {
                        costsTextBuilder.append(String.format(costBespokeText, caseData.getBespokeCostDetails()
                            .getDetailText()));
                    }
                }
                break;


            }
        }
        return costsTextBuilder.toString();
    }

    protected String getFurtherHearingText(CaseData caseData){

        StringBuilder furtherHearingBuilder = new StringBuilder();
        if (nonNull(caseData.getAssistedOrderFurtherHearingToggle())
            && nonNull(caseData.getAssistedOrderFurtherHearingToggle().get(0))
            && caseData.getAssistedOrderFurtherHearingToggle().get(0).equals(FinalOrderShowToggle.SHOW)) {

            furtherHearingBuilder.append(String.format(furtherHearingTakePlaceAfterText,
                                                       getDateFormatted(
                                                           caseData.getAssistedOrderFurtherHearingDetails()
                                                               .getListFromDate())));
            if (nonNull(caseData.getAssistedOrderFurtherHearingDetails().getListToDate())) {
                furtherHearingBuilder.append(LINE_BREAKER);
                furtherHearingBuilder.append(String.format(furtherHearingTakePlaceBeforeText, getDateFormatted(
                    caseData.getAssistedOrderFurtherHearingDetails().getListToDate())));
            }

            if (nonNull(caseData.getAssistedOrderFurtherHearingDetails().getLengthOfNewHearing())) {
                if (caseData.getAssistedOrderFurtherHearingDetails()
                    .getLengthOfNewHearing().equals(LengthOfHearing.OTHER)) {
                    furtherHearingBuilder.append(LINE_BREAKER);
                    furtherHearingBuilder.append(String.format(furtherHearingLengthText,
                                                               String.format(furtherHearingLengthOther,
                                                               caseData.getAssistedOrderFurtherHearingDetails()
                                                                   .getLengthOfHearingOther().getLengthListOtherDays(),
                                                               caseData.getAssistedOrderFurtherHearingDetails()
                                                                   .getLengthOfHearingOther().getLengthListOtherHours(),
                                                               caseData.getAssistedOrderFurtherHearingDetails()
                                                                   .getLengthOfHearingOther()
                                                                                 .getLengthListOtherMinutes())));
                } else {
                    furtherHearingBuilder.append(LINE_BREAKER);
                    furtherHearingBuilder.append(String.format(furtherHearingLengthText,
                                                 caseData.getAssistedOrderFurtherHearingDetails()
                                                     .getLengthOfNewHearing().getDisplayedValue()));
                }
            }

            if(nonNull(caseData.getAssistedOrderFurtherHearingDetails().getAlternativeHearingLocation())){
                furtherHearingBuilder.append(LINE_BREAKER);
                furtherHearingBuilder.append(String.format(furtherHearingAlternativeHearingText,
                                                           caseData.getAssistedOrderFurtherHearingDetails()
                                                               .getAlternativeHearingLocation().getListItems()
                                                               .get(0).getLabel()));

            }

            if(nonNull(caseData.getAssistedOrderFurtherHearingDetails().getHearingMethods())){
                furtherHearingBuilder.append(LINE_BREAKER);
                furtherHearingBuilder.append(String.format(furtherHearingMethodHearingText,
                                                           caseData.getAssistedOrderFurtherHearingDetails()
                                                               .getHearingMethods().getDisplayedValue()));
            }
            return furtherHearingBuilder.toString();
        }
        return null;
    }

    private String getPermissionToAppealText(CaseData caseData) {
        StringBuilder permissionToAppealBuilder = new StringBuilder();
        if (nonNull(caseData.getAssistedOrderAppealToggle())
            && nonNull(caseData.getAssistedOrderAppealToggle().get(0))
            && caseData.getAssistedOrderAppealToggle().get(0).equals(FinalOrderShowToggle.SHOW)) {
            if(nonNull(caseData.getAssistedOrderAppealDetails())){
                permissionToAppealBuilder.append(String.format(permissionToAppealText,
                                                               caseData.getAssistedOrderAppealDetails()
                                                                   .getAppealOrigin().getDisplayedValue(),
                                                               caseData.getAssistedOrderAppealDetails()
                                                                   .getPermissionToAppeal().getDisplayedValue()));

                if(nonNull(caseData.getAssistedOrderAppealDetails().getReasonsText())){
                    permissionToAppealBuilder.append(LINE_BREAKER);
                    permissionToAppealBuilder.append(String.format(permissionToAppealReasonsText,
                                                                   caseData.getAssistedOrderAppealDetails()
                                                                       .getReasonsText()));
                }
            }
            return permissionToAppealBuilder.toString();
        }
        return null;
    }

    protected String getRecitalRecordedText(CaseData caseData) {
        StringBuilder recordedText = new StringBuilder();

        if(isNull(caseData.getAssistedOrderRecitals()) ||
            isNull(caseData.getAssistedOrderRecitals().get(0))) {
            return null;
        }else if(caseData.getAssistedOrderRecitals().get(0).equals(FinalOrderShowToggle.SHOW)){
            if(nonNull(caseData.getAssistedOrderRecitalsRecorded())) {
                    recordedText.append(String.format(recitalRecordedText,
                                                           caseData.getAssistedOrderRecitalsRecorded().getText()));

            }
        }
        return recordedText.toString();
    }

    protected String generalJudgeHeardFromText(CaseData caseData) {
        StringBuilder judgeHeardFromBuilder = new StringBuilder();
        if(isNull(caseData.getAssistedOrderJudgeHeardFrom()) ||
            isNull(caseData.getAssistedOrderJudgeHeardFrom().get(0))) {
            return null;
        }else if(caseData.getAssistedOrderJudgeHeardFrom().get(0).equals(FinalOrderShowToggle.SHOW)){
            if(nonNull(caseData.getAssistedOrderRepresentation()) && caseData.getAssistedOrderRepresentation()
                .getRepresentationType().equals(HeardFromRepresentationTypes.CLAIMANT_AND_DEFENDANT)){
                //Both Attended
                if(!caseData.getAssistedOrderRepresentation()
                    .getClaimantDefendantRepresentation().getDefendantRepresentation().equals(
                        DefendantRepresentationType.DEFENDANT_NOT_ATTENDING)
                    && !caseData.getAssistedOrderRepresentation()
                    .getClaimantDefendantRepresentation().getClaimantRepresentation().equals(CLAIMANT_NOT_ATTENDING)) {
                    judgeHeardFromBuilder.append(String.format(
                        CLAIMANT_DEFENDANT_BOTH_ATTENDED_TEXT,
                        caseData.getAssistedOrderRepresentation()
                                                               .getClaimantDefendantRepresentation()
                                                               .getClaimantRepresentation().getDisplayedValue()
                            .toLowerCase(),
                        caseData.getAssistedOrderRepresentation()
                                                                .getClaimantDefendantRepresentation()
                                                                .getDefendantRepresentation().getDisplayedValue().toLowerCase()));

                } else if(caseData.getAssistedOrderRepresentation()
                    .getClaimantDefendantRepresentation().getDefendantRepresentation().equals(
                        DefendantRepresentationType.DEFENDANT_NOT_ATTENDING)
                    && !caseData.getAssistedOrderRepresentation()
                    .getClaimantDefendantRepresentation().getClaimantRepresentation().equals(CLAIMANT_NOT_ATTENDING)) {
                    //Claimant Attended Defendant Not ATTENDED
                    judgeHeardFromBuilder.append(String.format(CLAIMANT_OR_DEFENDANT_ATTENDED_TEXT,
                                                            caseData.getAssistedOrderRepresentation()
                                                                .getClaimantDefendantRepresentation()
                                                                .getClaimantRepresentation().getDisplayedValue()));
                    judgeHeardFromBuilder.append(LINE_BREAKER);
                    judgeHeardFromBuilder.append(DEFENDANT_NOT_ATTENDED_TEXT);
                    judgeHeardFromBuilder.append(getJudgeSatisfiedText(caseData.getAssistedOrderRepresentation()
                                                                        .getClaimantDefendantRepresentation()
                                                                        .getHeardFromDefendantNotAttend()
                                                                        .getListDef()));

                }else if(!caseData.getAssistedOrderRepresentation()
                    .getClaimantDefendantRepresentation().getDefendantRepresentation().equals(
                        DefendantRepresentationType.DEFENDANT_NOT_ATTENDING)
                    && caseData.getAssistedOrderRepresentation()
                    .getClaimantDefendantRepresentation().getClaimantRepresentation().equals(CLAIMANT_NOT_ATTENDING)) {
                    //Claimant Not ATTENDED, Defendant Attended
                    judgeHeardFromBuilder.append(CLAIMANT_NOT_ATTENDED_TEXT);
                    judgeHeardFromBuilder.append(getJudgeSatisfiedText(caseData.getAssistedOrderRepresentation()
                                                                        .getClaimantDefendantRepresentation()
                                                                        .getHeardFromClaimantNotAttend()
                                                                        .getListClaim()));
                    judgeHeardFromBuilder.append(LINE_BREAKER);
                    judgeHeardFromBuilder.append(String.format(CLAIMANT_OR_DEFENDANT_ATTENDED_TEXT,
                                                            caseData.getAssistedOrderRepresentation()
                                                                .getClaimantDefendantRepresentation()
                                                                .getDefendantRepresentation()
                                                                .getDisplayedValue()));
                }else if(caseData.getAssistedOrderRepresentation()
                    .getClaimantDefendantRepresentation().getDefendantRepresentation().equals(
                        DefendantRepresentationType.DEFENDANT_NOT_ATTENDING)
                    && caseData.getAssistedOrderRepresentation()
                    .getClaimantDefendantRepresentation().getClaimantRepresentation().equals(CLAIMANT_NOT_ATTENDING)){
                    //Both Not Attended
                    judgeHeardFromBuilder.append(CLAIMANT_NOT_ATTENDED_TEXT);
                    judgeHeardFromBuilder.append(getJudgeSatisfiedText(caseData.getAssistedOrderRepresentation()
                                                                        .getClaimantDefendantRepresentation()
                                                                        .getHeardFromClaimantNotAttend()
                                                                        .getListClaim()));
                    judgeHeardFromBuilder.append(LINE_BREAKER);
                    judgeHeardFromBuilder.append(DEFENDANT_NOT_ATTENDED_TEXT);
                    judgeHeardFromBuilder.append(getJudgeSatisfiedText(caseData.getAssistedOrderRepresentation()
                                                                        .getClaimantDefendantRepresentation()
                                                                        .getHeardFromDefendantNotAttend()
                                                                        .getListDef()));
                }
            }else if(nonNull(caseData.getAssistedOrderRepresentation()) && caseData.getAssistedOrderRepresentation()
                .getRepresentationType().equals(HeardFromRepresentationTypes.OTHER_REPRESENTATION)){
                judgeHeardFromBuilder.append(String.format(
                    AssistedOrderFormGenerator.judgeHeardFromText,
                    caseData.getAssistedOrderRepresentation()
                                                           .getOtherRepresentation().getDetailText()));
            }
            if(nonNull(caseData.getAssistedOrderRepresentation())
                && caseData.getAssistedOrderRepresentation().getTypeRepresentationJudgePapersList()!=null
                && caseData.getAssistedOrderRepresentation().getTypeRepresentationJudgePapersList().get(0)
                .equals(CONSIDERED)) {
                judgeHeardFromBuilder.append(LINE_BREAKER);
                judgeHeardFromBuilder.append(judgeConsiderPapersText);
            }

        }
        return judgeHeardFromBuilder.toString();
    }

    protected String getOrderMadeOnText(CaseData caseData) {
        StringBuilder orderMadeOnText = new StringBuilder();
        if (caseData.getOrderMadeOnOption().equals(OrderMadeOnTypes.COURTS_INITIATIVE)) {
            orderMadeOnText.append(caseData.getOrderMadeOnOwnInitiative().getDetailText());
            orderMadeOnText.append(DATE_FORMATTER.format(caseData.getOrderMadeOnOwnInitiative().getDate()));
        } else if (caseData.getOrderMadeOnOption().equals(OrderMadeOnTypes.WITHOUT_NOTICE)) {
            orderMadeOnText.append(caseData.getOrderMadeOnWithOutNotice().getDetailText());
            orderMadeOnText.append(DATE_FORMATTER.format(caseData.getOrderMadeOnWithOutNotice().getDate()));
        } else if (caseData.getOrderMadeOnOption().equals(OrderMadeOnTypes.NONE)) {
            orderMadeOnText.append(ORDER_MADE_ON_NONE_TEXT);
        }
        return orderMadeOnText.toString();
    }

    protected String getOrderMadeDate(CaseData caseData) {

        if (isNull(caseData.getAssistedOrderMadeDateHeardDetails())
            || isNull(caseData.getAssistedOrderMadeDateHeardDetails().getDate())) {
            return null;
        }
        return getDateFormatted(caseData.getAssistedOrderMadeDateHeardDetails().getDate());
    }

    protected String getReasonText(CaseData caseData) {
        if (isNull(caseData.getAssistedOrderGiveReasonsYesNo())
            || caseData.getAssistedOrderGiveReasonsYesNo().equals(YesOrNo.NO)
            || isNull(caseData.getAssistedOrderGiveReasonsDetails().getReasonsText())) {
            return null;
        } else {
            StringBuilder reasonBuilder = new StringBuilder();
            reasonBuilder.append(caseData.getAssistedOrderGiveReasonsDetails().getReasonsText());
            return reasonBuilder.toString();
        }
    }

    protected String getFileName(DocmosisTemplates template) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FILE_TIMESTAMP_FORMAT);
        return String.format(template.getDocumentTitle(),
                LocalDateTime.now().format(formatter));
    }

    @SuppressWarnings("unchecked")
    protected String getReference(CaseDetails caseData, String refKey) {
        if (nonNull(caseData.getData().get("solicitorReferences"))) {
            return ((Map<String, String>) caseData.getData().get("solicitorReferences")).get(refKey);
        }
        return null;
    }

    protected String getCaseNumberFormatted(CaseData caseData) {
        String[] parts = caseData.getCcdCaseReference().toString().split("(?<=\\G.{4})");
        return String.join("-", parts);
    }

    protected String getDateFormatted(LocalDate date) {
        if (isNull(date)) {
            return null;
        }
        return DateFormatHelper.formatLocalDate(date, "dd/MMM/yyyy");
    }

    protected String getCostText(AssistedOrderCost assistedOrderCost){
        if(nonNull(assistedOrderCost.getCostAmount())) {
           return String.format(costAmount, assistedOrderCost.formatCaseAmountToPounds());
        }
        return "";
    }

    protected  String getJudgeSatisfiedText(ClaimantDefendantNotAttendingType attendingType){
        if(nonNull(attendingType)){
            if(attendingType.equals(ClaimantDefendantNotAttendingType.SATISFIED_REASONABLE_TO_PROCEED)) {
                return JUDGE_SATISFIED_TO_PROCEED_TEXT;
            }else if(attendingType.equals(ClaimantDefendantNotAttendingType.SATISFIED_NOTICE_OF_TRIAL)) {
                return JUDGE_SATISFIED_NOTICE_OF_TRIAL_TEXT;
            }else if(attendingType.equals(ClaimantDefendantNotAttendingType.NOT_SATISFIED_NOTICE_OF_TRIAL)) {
                return JUDGE_NOT_SATISFIED_NOTICE_OF_TRIAL_TEXT;
            }
        }
        return "";
    }
    protected String getIsProtectionDateText(AssistedOrderCost assistedOrderCost){
        StringBuilder assistedCostBuilder = new StringBuilder();

        if(nonNull(assistedOrderCost.getCostPaymentDeadLine())) {
            assistedCostBuilder.append(LINE_BREAKER);
            assistedCostBuilder.append(
                String.format(costToBePaidBy, getDateFormatted(assistedOrderCost.getCostPaymentDeadLine())));
        }

        if(nonNull(assistedOrderCost.getIsPartyCostProtection())) {
            if(assistedOrderCost.getIsPartyCostProtection().equals(YesOrNo.YES)){
                assistedCostBuilder.append(LINE_BREAKER);
                assistedCostBuilder.append(costPartyHasBenefit);
            }else {
                assistedCostBuilder.append(LINE_BREAKER);
                assistedCostBuilder.append(costPartyHasNoBenefit);
            }
        }
        return assistedCostBuilder.toString();
    }
    protected DocmosisTemplates getTemplate() {
        return ASSISTED_ORDER_FORM;
    }
}
