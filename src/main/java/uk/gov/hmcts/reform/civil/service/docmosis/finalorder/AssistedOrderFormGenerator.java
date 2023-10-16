package uk.gov.hmcts.reform.civil.service.docmosis.finalorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.AppealOriginTypes;
import uk.gov.hmcts.reform.civil.enums.dq.AssistedOrderCostDropdownList;
import uk.gov.hmcts.reform.civil.enums.dq.DefendantRepresentationType;
import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderShowToggle;
import uk.gov.hmcts.reform.civil.enums.dq.LengthOfHearing;
import uk.gov.hmcts.reform.civil.enums.dq.OrderMadeOnTypes;
import uk.gov.hmcts.reform.civil.enums.dq.PermissionToAppealTypes;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.AssistedOrderForm;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.civil.utils.MonetaryConversions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.ClaimantRepresentationType.CLAIMANT_NOT_ATTENDING;
import static uk.gov.hmcts.reform.civil.enums.dq.FinalOrderConsideredToggle.CONSIDERED;
import static uk.gov.hmcts.reform.civil.enums.dq.HeardFromRepresentationTypes.CLAIMANT_AND_DEFENDANT;
import static uk.gov.hmcts.reform.civil.enums.dq.HeardFromRepresentationTypes.OTHER_REPRESENTATION;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.ASSISTED_ORDER_FORM;

@Service
@RequiredArgsConstructor
public class AssistedOrderFormGenerator implements TemplateDataGenerator<AssistedOrderForm> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final DocmosisService docmosisService;


    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(" d MMMM yyyy");
    private static final String FILE_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public CaseDocument generate(CaseData caseData, String authorisation) {

        AssistedOrderForm templateData = getTemplateData(caseData, authorisation);
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

    private AssistedOrderForm getTemplateData(CaseData caseData, String authorisation) {

        return AssistedOrderForm.builder()
                .caseNumber(getCaseNumberFormatted(caseData))
                .claimant1Name(caseData.getClaimant1PartyName())
                .defendant1Name(caseData.getDefendant1PartyName())
                .defendant2Name(caseData.getIsMultiParty().equals(YesOrNo.YES) ? caseData.getDefendant2PartyName() : null)
                .caseName(caseData.getCaseNameHmctsInternal())
                .courtLocation(caseData.getLocationName())
                .receivedDate(getDateFormatted(LocalDate.now()))
                .judgeNameTitle(docmosisService.getJudgeNameTitle(authorisation))
                .isOrderMade(caseData.getAssistedOrderMadeSelection())
                .isSingleDate(caseData.getAssistedOrderMadeSelection().equals(YesOrNo.YES) && nonNull(caseData.getAssistedOrderMadeDateHeardDetails().getSingleDateSelection()))
                .orderMadeSingleDate(getOrderMadeSingleDate(caseData))
                .isDateRange(caseData.getAssistedOrderMadeSelection().equals(YesOrNo.YES) && nonNull(caseData.getAssistedOrderMadeDateHeardDetails().getDateRangeSelection()))
                .orderMadeDateRangeFrom(getOrderMadeDateRangeFrom(caseData))
                .orderMadeDateRangeTo(getOrderMadeDateRangeTo(caseData))
                .isBeSpokeRange(caseData.getAssistedOrderMadeSelection().equals(YesOrNo.YES) && nonNull(caseData.getAssistedOrderMadeDateHeardDetails().getBeSpokeRangeSelection()))
                .orderMadeBeSpokeText(getOrderMadeBeSpokeText(caseData))
                .judgeHeardFromShowHide(checkJudgeHeardFromToggle(caseData))
                .judgeHeardSelection(getJudgeHeardFromRepresentation(caseData))
                .claimantRepresentation(getClaimantRepresentation(caseData))
                .defendantRepresentation(getDefendantRepresentation(caseData))
                .defendantTwoRepresentation(getDefendantTwoRepresentation(caseData))
                .isDefendantTwoExists(caseData.getIsMultiParty().equals(YesOrNo.YES))
                .heardClaimantNotAttend(getHeardClaimantNotAttend(caseData))
                .heardDefendantNotAttend(getHeardDefendantNotAttend(caseData))
                .heardDefendantTwoNotAttend(getHeardDefendantTwoNotAttend(caseData))
                .isOtherRepresentation(nonNull(caseData.getAssistedOrderRepresentation())
                                       && caseData.getAssistedOrderRepresentation().getRepresentationType().equals(
                OTHER_REPRESENTATION))
                .otherRepresentationText(getOtherRepresentationText(caseData))
                .isJudgeConsidered(checkIsJudgeConsidered(caseData))
                .orderedText(caseData.getAssistedOrderOrderedThatText())
                .showRecitals(checkRecitalsToggle(caseData))
                .recitalRecordedText(nonNull(caseData.getAssistedOrderRecitalsRecorded()) ? caseData.getAssistedOrderRecitalsRecorded().getText() : null)
                .showFurtherHearing(checkFurtherHearingToggle(caseData))
                .checkListToDate(nonNull(caseData.getAssistedOrderFurtherHearingDetails())
                                     && nonNull(caseData.getAssistedOrderFurtherHearingDetails().getListToDate()) ? YesOrNo.YES : YesOrNo.NO)
            .furtherHearingListFromDate(getFurtherHearingListFromDate(caseData))
            .furtherHearingListToDate(getFurtherHearingListToDate(caseData))
            .furtherHearingMethod(nonNull(caseData.getAssistedOrderFurtherHearingDetails()) ? caseData.getAssistedOrderFurtherHearingDetails().getHearingMethods().name() : null)
            .furtherHearingDuration(getFurtherHearingDuration(caseData))
            .checkDatesToAvoid(nonNull(caseData.getAssistedOrderFurtherHearingDetails()) && caseData.getAssistedOrderFurtherHearingDetails().getDatesToAvoid().equals(YesOrNo.YES))
            .furtherHearingDatesToAvoid(getFurtherHearingDatesToAvoid(caseData))
            .furtherHearingLocation(getFurtherHearingLocation(caseData))
            .costSelection(caseData.getAssistedCostTypes().name())
            .summarilyAssessed(getSummarilyAssessed(caseData))
            .summarilyAssessedDate(getDateFormatted(getSummarilyAssessedDate(caseData)))
            .detailedAssessment(getDetailedAssessment(caseData))
            .interimPayment(getInterimPayment(caseData))
            .interimPaymentDate(getDateFormatted(getInterimPaymentDate(caseData)))
            .isQocsProtectionEnabled(checkIsQocsProtectionEnabled(caseData))
            .costsProtection(caseData.getPublicFundingCostsProtection())
            .showAppeal(checkAppealToggle(caseData))
            .claimantOrDefendantAppeal(getClaimantOrDefendantAppeal(caseData))
            .isAppealGranted(nonNull(caseData.getAssistedOrderAppealDetails())
                                 && caseData.getAssistedOrderAppealDetails().getPermissionToAppeal().name().equals(PermissionToAppealTypes.GRANTED.name()))
            .tableAorB(checkCircuitOrHighCourtJudge(caseData))
            .appealDate(getDateFormatted(getAppealDate(caseData)))
            .showInitiativeOrWithoutNotice(caseData.getOrderMadeOnOption().equals(OrderMadeOnTypes.COURTS_INITIATIVE)
                                               || caseData.getOrderMadeOnOption().equals(OrderMadeOnTypes.WITHOUT_NOTICE))
            .showInitiative(caseData.getOrderMadeOnOption().equals(OrderMadeOnTypes.COURTS_INITIATIVE))
            .orderMadeOnText(getOrderMadeOnText(caseData))
            .initiativeDate(getOrderMadeCourtInitiativeDate(caseData))
            .withoutNoticeDate(getOrderMadeCourtWithOutNoticeDate(caseData))
            .reasonsText(getReasonText(caseData))
                .build();
    }

    private String getOrderMadeDateRangeTo(CaseData caseData) {
        return (caseData.getAssistedOrderMadeSelection().equals(YesOrNo.YES)
            && nonNull(caseData.getAssistedOrderMadeDateHeardDetails().getDateRangeSelection()))
            ? getDateFormatted(caseData.getAssistedOrderMadeDateHeardDetails().getDateRangeSelection().getDateRangeTo()) : null;
    }

    private String getOrderMadeDateRangeFrom(CaseData caseData) {
        return (caseData.getAssistedOrderMadeSelection().equals(YesOrNo.YES)
            && nonNull(caseData.getAssistedOrderMadeDateHeardDetails().getDateRangeSelection()))
            ? getDateFormatted(caseData.getAssistedOrderMadeDateHeardDetails().getDateRangeSelection().getDateRangeFrom()) : null;
    }

    private String getOrderMadeBeSpokeText(CaseData caseData) {
        return (caseData.getAssistedOrderMadeSelection().equals(YesOrNo.YES)
            && nonNull(caseData.getAssistedOrderMadeDateHeardDetails().getBeSpokeRangeSelection()))
            ? caseData.getAssistedOrderMadeDateHeardDetails().getBeSpokeRangeSelection().getBeSpokeRangeText() : null;
    }

    private String getFurtherHearingListFromDate(CaseData caseData) {
        return nonNull(caseData.getAssistedOrderFurtherHearingDetails())
            ? getDateFormatted(caseData.getAssistedOrderFurtherHearingDetails().getListFromDate()): null;
    }

    private String getFurtherHearingListToDate(CaseData caseData) {
        return (nonNull(caseData.getAssistedOrderFurtherHearingDetails())
            && nonNull(caseData.getAssistedOrderFurtherHearingDetails().getListToDate()))
            ? getDateFormatted(caseData.getAssistedOrderFurtherHearingDetails().getListToDate()) : null;
    }

    private String getFurtherHearingDatesToAvoid(CaseData caseData) {
        return nonNull(caseData.getAssistedOrderFurtherHearingDetails())
            && caseData.getAssistedOrderFurtherHearingDetails().getDatesToAvoid().equals(YesOrNo.YES)
            ? getDateFormatted(caseData.getAssistedOrderFurtherHearingDetails().getDatesToAvoidDateDropdown().getDatesToAvoidDates()) : null;
    }

    private String getOrderMadeSingleDate(CaseData caseData) {
        return (caseData.getAssistedOrderMadeSelection().equals(YesOrNo.YES)
            && nonNull(caseData.getAssistedOrderMadeDateHeardDetails().getSingleDateSelection()))
            ? getDateFormatted(caseData.getAssistedOrderMadeDateHeardDetails().getSingleDateSelection().getSingleDate()) : null;
    }

    private String getOtherRepresentationText(CaseData caseData) {
        return nonNull(caseData.getAssistedOrderRepresentation())
            && caseData.getAssistedOrderRepresentation().getRepresentationType().equals(
            OTHER_REPRESENTATION) ? caseData.getAssistedOrderRepresentation().getOtherRepresentation().getDetailText() : null;
    }

    private Boolean checkIsQocsProtectionEnabled(CaseData caseData) {
        return nonNull(caseData.getAssistedOrderMakeAnOrderForCosts())
            && nonNull(caseData.getAssistedOrderMakeAnOrderForCosts().getMakeAnOrderForCostsYesOrNo())
            && caseData.getAssistedOrderMakeAnOrderForCosts().getMakeAnOrderForCostsYesOrNo().equals(
            YES);
    }

    private Boolean checkIsJudgeConsidered(CaseData caseData) {
        return nonNull(caseData.getAssistedOrderRepresentation())
            && nonNull(caseData.getAssistedOrderRepresentation().getTypeRepresentationJudgePapersList())
            && caseData.getAssistedOrderRepresentation().getTypeRepresentationJudgePapersList()
            .get(0).getDisplayedValue().equals(CONSIDERED.getDisplayedValue());
    }

    private String getOrderMadeCourtWithOutNoticeDate(CaseData caseData) {
        if (caseData.getOrderMadeOnOption().equals(OrderMadeOnTypes.WITHOUT_NOTICE)) {
            return DATE_FORMATTER.format(caseData.getOrderMadeOnWithOutNotice().getDate());
        }
        return "";
    }

    private String getOrderMadeCourtInitiativeDate(CaseData caseData) {
        if (caseData.getOrderMadeOnOption().equals(OrderMadeOnTypes.COURTS_INITIATIVE)) {
            return DATE_FORMATTER.format(caseData.getOrderMadeOnOwnInitiative().getDate());
        }
        return "";
    }

    private LocalDate getAppealDate(CaseData caseData) {
        if (nonNull(caseData.getAssistedOrderAppealDetails())
            && caseData.getAssistedOrderAppealDetails().getPermissionToAppeal().name().equals(PermissionToAppealTypes.GRANTED.name())) {
            if (caseData.getAssistedOrderAppealDetails().getAppealTypeChoicesForGranted().getAssistedOrderAppealJudgeSelection()
                .equals(PermissionToAppealTypes.CIRCUIT_COURT_JUDGE)) {
                return caseData.getAssistedOrderAppealDetails().getAppealTypeChoicesForGranted().getAppealChoiceOptionA().getAppealGrantedRefusedDate();
            } else {
                return caseData.getAssistedOrderAppealDetails().getAppealTypeChoicesForGranted().getAppealChoiceOptionB().getAppealGrantedRefusedDate();
            }
        }
        if (nonNull(caseData.getAssistedOrderAppealDetails())
                        && caseData.getAssistedOrderAppealDetails().getPermissionToAppeal().name().equals(PermissionToAppealTypes.REFUSED.name())) {
            if (caseData.getAssistedOrderAppealDetails().getAppealTypeChoicesForGranted().getAssistedOrderAppealJudgeSelectionRefuse()
                .equals(PermissionToAppealTypes.CIRCUIT_COURT_JUDGE)) {
                return caseData.getAssistedOrderAppealDetails().getAppealTypeChoicesForGranted().getAppealChoiceOptionA().getAppealGrantedRefusedDate();
            } else {
                return caseData.getAssistedOrderAppealDetails().getAppealTypeChoicesForGranted().getAppealChoiceOptionB().getAppealGrantedRefusedDate();
            }
        }
        return null;
    }

    private String checkCircuitOrHighCourtJudge(CaseData caseData) {
        if (nonNull(caseData.getAssistedOrderAppealDetails())
        && caseData.getAssistedOrderAppealDetails().getPermissionToAppeal().name().equals(PermissionToAppealTypes.GRANTED.name())
        && caseData.getAssistedOrderAppealDetails().getAppealTypeChoicesForGranted().getAssistedOrderAppealJudgeSelection()
            .equals(PermissionToAppealTypes.CIRCUIT_COURT_JUDGE)) {
            return "A";
        }
        if (nonNull(caseData.getAssistedOrderAppealDetails())
            && caseData.getAssistedOrderAppealDetails().getPermissionToAppeal().name().equals(PermissionToAppealTypes.REFUSED.name())
            && caseData.getAssistedOrderAppealDetails().getAppealTypeChoicesForGranted().getAssistedOrderAppealJudgeSelection()
            .equals(PermissionToAppealTypes.CIRCUIT_COURT_JUDGE)) {
            return "A";
        }
        return "B";
    }

    private String getClaimantOrDefendantAppeal(CaseData caseData) {
        if (nonNull(caseData.getAssistedOrderAppealDetails()) && nonNull(caseData.getAssistedOrderAppealDetails().getAppealOrigin())) {
            if(caseData.getAssistedOrderAppealDetails().getAppealOrigin().name().equals(AppealOriginTypes.OTHER.name())) {
                return caseData.getAssistedOrderAppealDetails().getOtherOriginText();
            } else {
                return caseData.getAssistedOrderAppealDetails().getAppealOrigin().getDisplayedValue();
            }
        }
        return "";
    }

    private Boolean checkAppealToggle(CaseData caseData) {
        return (nonNull(caseData.getAssistedOrderAppealToggle())
            && nonNull(caseData.getAssistedOrderAppealToggle().get(0))
            && caseData.getAssistedOrderAppealToggle().get(0).equals(FinalOrderShowToggle.SHOW));
    }

    private String getSummarilyAssessed(CaseData caseData) {
        return nonNull(caseData.getAssistedOrderMakeAnOrderForCosts())
            && nonNull(caseData.getAssistedOrderMakeAnOrderForCosts().getMakeAnOrderForCostsList())
            && caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderCostsMakeAnOrderTopList().equals(
            AssistedOrderCostDropdownList.COSTS)
            ? populateSummarilyAssessedText(caseData) : null;
    }

    private LocalDate getSummarilyAssessedDate(CaseData caseData) {
        return nonNull(caseData.getAssistedOrderMakeAnOrderForCosts())
            && nonNull(caseData.getAssistedOrderMakeAnOrderForCosts().getMakeAnOrderForCostsList())
            && caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderCostsMakeAnOrderTopList().equals(
            AssistedOrderCostDropdownList.COSTS)
            ? caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderCostsFirstDropdownDate() : null;
    }

    public String populateSummarilyAssessedText(CaseData caseData) {
        if (caseData.getAssistedOrderMakeAnOrderForCosts().getMakeAnOrderForCostsList().equals(
            AssistedOrderCostDropdownList.CLAIMANT)) {
            return format(
                "The claimant shall pay the defendant's costs (both fixed and summarily assessed as appropriate) "
                    + "in the sum of %s. Such a sum shall be made by 4pm on",
                MonetaryConversions.penniesToPounds(caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderCostsFirstDropdownAmount()));
        } else {
            return format(
                "The defendant shall pay the claimant's costs (both fixed and summarily assessed as appropriate) "
                    + "in the sum of %s. Such a sum shall be made by 4pm on",
                MonetaryConversions.penniesToPounds(caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderCostsFirstDropdownAmount()));
        }
    }

    private String getDetailedAssessment(CaseData caseData) {
        return nonNull(caseData.getAssistedOrderMakeAnOrderForCosts())
            && nonNull(caseData.getAssistedOrderMakeAnOrderForCosts().getMakeAnOrderForCostsList())
            && caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderCostsMakeAnOrderTopList().equals(
            AssistedOrderCostDropdownList.SUBJECT_DETAILED_ASSESSMENT)
            ? populateDetailedAssessmentText(caseData) : null;
    }

    public String populateDetailedAssessmentText(CaseData caseData) {
        String standardOrIndemnity;
        if (caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderAssessmentSecondDropdownList1().equals(
            AssistedOrderCostDropdownList.INDEMNITY_BASIS)) {
            standardOrIndemnity = "on the indemnity basis if not agreed";
        } else {
            standardOrIndemnity = "on the standard basis if not agreed";
        }

        if (caseData.getAssistedOrderMakeAnOrderForCosts().getMakeAnOrderForCostsList().equals(AssistedOrderCostDropdownList.CLAIMANT)) {
            return format(
                "The claimant shall pay the defendant's costs to be subject to a detailed assessment %s",
                standardOrIndemnity
            );
        }
        return format(
            "The defendant shall pay the claimant's costs to be subject to a detailed assessment %s",
            standardOrIndemnity
        );
    }

    private String getInterimPayment(CaseData caseData) {
        return nonNull(caseData.getAssistedOrderMakeAnOrderForCosts())
            && nonNull(caseData.getAssistedOrderMakeAnOrderForCosts().getMakeAnOrderForCostsList())
            && caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderCostsMakeAnOrderTopList().equals(
            AssistedOrderCostDropdownList.SUBJECT_DETAILED_ASSESSMENT)
            && caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderAssessmentSecondDropdownList2().equals(
            AssistedOrderCostDropdownList.YES)
            ? populateInterimPaymentText(caseData) : null;
    }

    public String populateInterimPaymentText(CaseData caseData) {
        return format(
            "An interim payment of £%s on account of costs shall be paid by 4pm on ",
            MonetaryConversions.penniesToPounds(caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderAssessmentThirdDropdownAmount()));
    }

    private LocalDate getInterimPaymentDate(CaseData caseData) {
        return nonNull(caseData.getAssistedOrderMakeAnOrderForCosts())
            && nonNull(caseData.getAssistedOrderMakeAnOrderForCosts().getMakeAnOrderForCostsList())
            && caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderCostsMakeAnOrderTopList().equals(
            AssistedOrderCostDropdownList.SUBJECT_DETAILED_ASSESSMENT)
            ? caseData.getAssistedOrderMakeAnOrderForCosts().getAssistedOrderAssessmentThirdDropdownDate() : null;
    }


    private String getFurtherHearingLocation(CaseData caseData) {
        if (nonNull(caseData.getAssistedOrderFurtherHearingDetails())
        && (caseData.getAssistedOrderFurtherHearingDetails().getHearingLocationList()
            .getValue().getLabel().equalsIgnoreCase("Other location"))) {
            return caseData.getAssistedOrderFurtherHearingDetails().getAlternativeHearingLocation().getValue().getLabel();
        }
        return caseData.getLocationName();
    }

    private String getFurtherHearingDuration(CaseData caseData) {
        StringBuilder otherDuration = new StringBuilder();
        if (nonNull(caseData.getAssistedOrderFurtherHearingDetails())
            && caseData.getAssistedOrderFurtherHearingDetails().getLengthOfNewHearing().equals(LengthOfHearing.OTHER)) {
            otherDuration.append(caseData.getAssistedOrderFurtherHearingDetails()
                                     .getLengthOfHearingOther().getLengthListOtherDays()).append(" days ")
                .append(caseData.getAssistedOrderFurtherHearingDetails()
                    .getLengthOfHearingOther().getLengthListOtherHours()).append(" hours ")
                .append(caseData.getAssistedOrderFurtherHearingDetails().getLengthOfHearingOther()
                            .getLengthListOtherMinutes()).append(" minutes ");
            return otherDuration.toString();
        }
        return nonNull(caseData.getAssistedOrderFurtherHearingDetails())
            ? caseData.getAssistedOrderFurtherHearingDetails().getLengthOfNewHearing().getDisplayedValue() : null;
    }

    private Boolean checkFurtherHearingToggle(CaseData caseData) {
        return (nonNull(caseData.getAssistedOrderFurtherHearingToggle())
            && nonNull(caseData.getAssistedOrderFurtherHearingToggle().get(0))
            && caseData.getAssistedOrderFurtherHearingToggle().get(0).equals(FinalOrderShowToggle.SHOW));
    }

    private String getHeardClaimantNotAttend(CaseData caseData) {
        if (nonNull(caseData.getAssistedOrderRepresentation())
            && caseData.getAssistedOrderRepresentation().getRepresentationType().equals(CLAIMANT_AND_DEFENDANT)
            && caseData.getAssistedOrderRepresentation().getClaimantDefendantRepresentation()
            .getClaimantRepresentation().equals(CLAIMANT_NOT_ATTENDING)) {
            return caseData.getAssistedOrderRepresentation().getClaimantDefendantRepresentation()
                .getHeardFromClaimantNotAttend().getListClaim().getDisplayedValue();
        }
        return null;
    }

    private String getHeardDefendantNotAttend(CaseData caseData) {
        if (nonNull(caseData.getAssistedOrderRepresentation())
            && caseData.getAssistedOrderRepresentation().getRepresentationType().equals(CLAIMANT_AND_DEFENDANT)
            && caseData.getAssistedOrderRepresentation().getClaimantDefendantRepresentation().getDefendantRepresentation()
            .equals(DefendantRepresentationType.DEFENDANT_NOT_ATTENDING)) {
            return caseData.getAssistedOrderRepresentation().getClaimantDefendantRepresentation().getHeardFromDefendantNotAttend()
                .getListDef().getDisplayedValue();
        }
        return null;
    }

    private String getHeardDefendantTwoNotAttend(CaseData caseData) {
        if (caseData.getIsMultiParty().equals(YesOrNo.YES)
            && nonNull(caseData.getAssistedOrderRepresentation())
            && caseData.getAssistedOrderRepresentation().getRepresentationType().equals(CLAIMANT_AND_DEFENDANT)
            && caseData.getAssistedOrderRepresentation().getClaimantDefendantRepresentation()
            .getDefendantTwoRepresentation().equals(DefendantRepresentationType.DEFENDANT_NOT_ATTENDING)) {
            return caseData.getAssistedOrderRepresentation().getClaimantDefendantRepresentation().getHeardFromDefendantTwoNotAttend()
                .getListDefTwo().getDisplayedValue();
        }
        return null;
    }

    private String getClaimantRepresentation(CaseData caseData) {
        if (nonNull(caseData.getAssistedOrderRepresentation())
            && caseData.getAssistedOrderRepresentation().getRepresentationType().equals(CLAIMANT_AND_DEFENDANT)) {
            return caseData.getAssistedOrderRepresentation().getClaimantDefendantRepresentation().getClaimantRepresentation().getDisplayedValue();
        }
        return null;
    }

    private String getDefendantRepresentation(CaseData caseData) {
        if (nonNull(caseData.getAssistedOrderRepresentation())
            && caseData.getAssistedOrderRepresentation().getRepresentationType().equals(CLAIMANT_AND_DEFENDANT)) {
            return caseData.getAssistedOrderRepresentation().getClaimantDefendantRepresentation().getDefendantRepresentation().getDisplayedValue();
        }
        return null;
    }

    private String getDefendantTwoRepresentation(CaseData caseData) {
        if (caseData.getIsMultiParty().equals(YesOrNo.YES)
            && nonNull(caseData.getAssistedOrderRepresentation())
            && caseData.getAssistedOrderRepresentation().getRepresentationType().equals(CLAIMANT_AND_DEFENDANT)) {
            return caseData.getAssistedOrderRepresentation().getClaimantDefendantRepresentation().getDefendantTwoRepresentation().getDisplayedValue();
        }
        return null;
    }

    private String getJudgeHeardFromRepresentation(CaseData caseData) {
        if (nonNull(caseData.getAssistedOrderRepresentation())) {
            return caseData.getAssistedOrderRepresentation().getRepresentationType().getDisplayedValue();
        }
        return null;
    }

    private boolean checkJudgeHeardFromToggle(CaseData caseData) {
        return (nonNull(caseData.getAssistedOrderJudgeHeardFrom())
            && nonNull(caseData.getAssistedOrderJudgeHeardFrom().get(0))
            && caseData.getAssistedOrderJudgeHeardFrom().get(0).equals(FinalOrderShowToggle.SHOW));
    }

    private boolean checkRecitalsToggle(CaseData caseData) {

        return (nonNull(caseData.getAssistedOrderRecitals())
            && nonNull(caseData.getAssistedOrderRecitals().get(0))
            && caseData.getAssistedOrderRecitals().get(0).equals(FinalOrderShowToggle.SHOW));
    }

    protected String getOrderMadeOnText(CaseData caseData) {
        if (caseData.getOrderMadeOnOption().equals(OrderMadeOnTypes.COURTS_INITIATIVE)) {
            return caseData.getOrderMadeOnOwnInitiative().getDetailText();
        } else if (caseData.getOrderMadeOnOption().equals(OrderMadeOnTypes.WITHOUT_NOTICE)) {
            return caseData.getOrderMadeOnWithOutNotice().getDetailText();
        } else {
            return "";
        }
    }

    protected String getOrderMadeDate(CaseData caseData) {
        if (nonNull(caseData.getAssistedOrderMadeDateHeardDetails())
            && nonNull(caseData.getAssistedOrderMadeDateHeardDetails().getSingleDateSelection())) {
            return getDateFormatted(caseData.getAssistedOrderMadeDateHeardDetails().getSingleDateSelection().getSingleDate());
        }
        return null;
    }

    protected String getReasonText(CaseData caseData) {
        if (isNull(caseData.getAssistedOrderGiveReasonsYesNo())
            || caseData.getAssistedOrderGiveReasonsYesNo().equals(YesOrNo.NO)
            || isNull(caseData.getAssistedOrderGiveReasonsDetails().getReasonsText())) {
            return null;
        } else {
            return (caseData.getAssistedOrderGiveReasonsDetails().getReasonsText());
        }
    }

    protected String getFileName(DocmosisTemplates template) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FILE_TIMESTAMP_FORMAT);
        return String.format(template.getDocumentTitle(),
                LocalDateTime.now().format(formatter));
    }


    protected String getCaseNumberFormatted(CaseData caseData) {
        String[] parts = caseData.getCcdCaseReference().toString().split("(?<=\\G.{4})");
        return String.join("-", parts);
    }

    protected String getDateFormatted(LocalDate date) {
        if (isNull(date)) {
            return null;
        }
        return DateFormatHelper.formatLocalDate(date, " d MMMM yyyy");
    }

    protected DocmosisTemplates getTemplate() {
        return ASSISTED_ORDER_FORM;
    }
}
