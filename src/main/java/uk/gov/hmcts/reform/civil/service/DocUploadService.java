package uk.gov.hmcts.reform.civil.service;

import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRole;
import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRolesResource;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.UploadDocumentByType;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;
import uk.gov.hmcts.reform.civil.utils.ElementUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocUploadService {

    Logger log = LoggerFactory.getLogger(CoreCaseUserService.class);

    private final CoreCaseUserService coreCaseUserService;

    public String getUserRole(String caseId) {
        CaseAssignedUserRolesResource userRoles = coreCaseUserService.getUserRoles(caseId);
        Optional<CaseAssignedUserRole> role = userRoles.getCaseAssignedUserRoles().stream()
                .filter(x->!x.getCaseRole().equals(CaseRole.CREATOR.getFormattedName())
                && ArrayUtils.contains(CaseRole.values(), x.getCaseRole()))
                .findFirst();
        return role.map(CaseAssignedUserRole::getCaseRole).orElse(null);
    }

    public List<Element<CaseDocument>> addDocument(List<Element<Document>> source, List<Element<CaseDocument>> target, String role, String documentType) {
        if (Objects.isNull(source) || source.isEmpty()) {
            return target;
        }
        if (Objects.isNull(target)) {
            target = new ArrayList<>();
        }
        target.addAll(source.stream()
                .map(doc -> ElementUtils.element(CaseDocument.builder()
                        .documentLink(doc.getValue()
                                .toBuilder().categoryID(AssignCategoryId.APPLICATIONS)
                                .build())
                        .documentName(documentType)
                        .createdBy(role)
                        .createdDatetime(LocalDateTime.now()).build()))
                .toList());
        return target;
    }

    public String getDocumentType(CaseEvent event) {
        switch (event) {
            case INITIATE_GENERAL_APPLICATION:
                return "Supporting evidence";
            case RESPOND_TO_JUDGE_ADDITIONAL_INFO:
                return "Additional information";
            case RESPOND_TO_JUDGE_DIRECTIONS:
                return "Directions Order Documents";
            default:
                return "Unsupported event";
        }
    }
}
