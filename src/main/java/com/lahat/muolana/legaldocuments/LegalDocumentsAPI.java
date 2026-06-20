package com.lahat.muolana.legaldocuments;

import com.lahat.muolana.legaldocuments.domain.LegalDocumentService;
import org.springframework.stereotype.Component;

@Component
public class LegalDocumentsAPI {

    private final LegalDocumentService documentService;

    public LegalDocumentsAPI(LegalDocumentService documentService) {
        this.documentService = documentService;
    }

    public long countIngested() {
        return documentService.countIngested();
    }
}
