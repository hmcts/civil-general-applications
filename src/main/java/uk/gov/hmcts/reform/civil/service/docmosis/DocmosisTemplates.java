package uk.gov.hmcts.reform.civil.service.docmosis;

public enum DocmosisTemplates {
    DIRECTION_ORDER("Directionorder.docx", "Direction_order.pdf"),
    REQUEST_FOR_INFORMATION("Requestforinformation.docx", "Request_for_information.pdf");

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
