package uk.gov.hmcts.reform.civil.service.docmosis;

public enum DocmosisTemplates {
    GENERAL_ORDER("CV-UNS-GAP-ENG-01068.docx", "General_order_for_application_%s.pdf"),
    DIRECTION_ORDER("CV-UNS-GAP-ENG-01073.docx", "Directions_order_for_application_%s.pdf"),
    DISMISSAL_ORDER("CV-UNS-GAP-ENG-01067.docx", "Dismissal_order_for_application_%s.pdf"),
    REQUEST_FOR_INFORMATION("CV-UNS-GAP-ENG-01072.docx", "Request_for_information_for_application_%s.pdf"),
    HEARING_ORDER("CV-UNS-GAP-ENG-01069.docx", "Hearing_order_for_application_%s.pdf"),
    HEARING_APPLICATION("CV-UNS-GAP-ENG-01074.docx", "Application_Hearing_Notice_%s.pdf"),
    WRITTEN_REPRESENTATION_SEQUENTIAL("CV-UNS-GAP-ENG-01070.docx",
                                      "Order_Written_Representation_Sequential_for_application_%s.pdf"),
    WRITTEN_REPRESENTATION_CONCURRENT("CV-UNS-GAP-ENG-01071.docx",
                                      "Order_Written_Representation_Concurrent_for_application_%s.pdf"),
    FREE_FORM_ORDER("CV-UNS-GAP-ENG-01075.docx", GENERAL_ORDER.getDocumentTitle()),
    ASSISTED_ORDER_FORM("CV-UNS-GAP-ENG-01076.docx", GENERAL_ORDER.getDocumentTitle()),
    CONSENT_ORDER_FORM("CV-UNS-GAP-ENG-01078.docx", "Consent_order_for_application_%s.pdf"),
    GENERAL_APPLICATION_DRAFT("CV-UNS-GAP-ENG-01077.docx", "Draft_application_%s.pdf"),
    REQUEST_FOR_INFORMATION_SEND_TO_OTHER_PARTY("CV-SPC-GAP-ENG-SEND-TO-OTHER-PARTY.docx", "make-with-notice_%s.pdf"),
    RESPOND_FOR_INFORMATION("CV-UNS-GAP-ENG-01079.docx", "Respond_for_information_for_application_%s.pdf"),
    RESPOND_FOR_WRITTEN_REPRESENTATION("CV-SPC-GAP-ENG-01080.docx", "Respond_for_written_representation_for_application_%s.pdf"),
    POST_JUDGE_REQUEST_FOR_INFORMATION_ORDER_LIP("CV-UNS-GAP-ENG-01080.docx", "Post_judge_request_for_information_order_%s.pdf");
    private final String template;
    private final String documentTitle;

    DocmosisTemplates(String template, String documentTitle) {
        this.template = template;
        this.documentTitle = documentTitle;
    }

    public String getTemplate() {
        return template;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }
}
