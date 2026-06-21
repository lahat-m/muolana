package com.lahat.muolana.legaldocuments.web;

import com.lahat.muolana.cloud.DocumentStorageService;
import com.lahat.muolana.legaldocuments.domain.*;
import com.lahat.muolana.legaldocuments.web.dtos.RejectRequest;
import com.lahat.muolana.legaldocuments.web.dtos.UploadRequest;
import com.lahat.muolana.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/admin/legal-documents")
@PreAuthorize("hasRole('ADMIN')")
class AdminLegalDocumentController {

    private final LegalDocumentService documentService;
    private final DocumentStorageService storageService;

    AdminLegalDocumentController(LegalDocumentService documentService,
                                 DocumentStorageService storageService) {
        this.documentService = documentService;
        this.storageService = storageService;
    }

    private static final Set<String> ALLOWED = Set.of("pdf", "docx");

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<LegalDocumentVM> upload(
            @Valid @RequestPart("metadata") UploadRequest req,
            @RequestPart("file") MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED.contains(ext)) {
            return ResponseEntity.badRequest().build();
        }
        String objectKey = storageService.store(file.getBytes(), originalName);
        UploadDocumentCmd cmd = new UploadDocumentCmd(
                req.title(), req.shortName(),
                DocumentCategory.valueOf(req.category().toUpperCase()),
                req.versionLabel(), req.sourceUrl(), objectKey);
        LegalDocumentVM doc = documentService.upload(cmd);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(doc.id()).toUri();
        return ResponseEntity.created(location).body(doc);
    }

    @GetMapping
    ResponseEntity<PageResponse<LegalDocumentVM>> listDocuments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(documentService.list(status, category, pageable)));
    }

    @GetMapping("/{docId}")
    ResponseEntity<LegalDocumentVM> getDocument(@PathVariable UUID docId) {
        return ResponseEntity.ok(documentService.getById(docId));
    }

    @GetMapping("/{docId}/file")
    ResponseEntity<Resource> downloadFile(@PathVariable UUID docId) {
        LegalDocumentVM doc = documentService.getById(docId);
        if (doc.filePath() == null || doc.filePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        String objectKey = doc.filePath();
        String filename = objectKey.substring(objectKey.lastIndexOf('/') + 1);
        boolean isPdf = filename.toLowerCase().endsWith(".pdf");
        MediaType type = isPdf ? MediaType.APPLICATION_PDF
                : MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        InputStream stream = storageService.download(objectKey);
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new InputStreamResource(stream) {
                    @Override
                    public long contentLength() { return -1; }
                });
    }

    @PatchMapping("/{docId}/verify")
    ResponseEntity<LegalDocumentVM> verifyDocument(@PathVariable UUID docId) {
        return ResponseEntity.ok(documentService.verify(docId));
    }

    @PatchMapping("/{docId}/reject")
    ResponseEntity<LegalDocumentVM> rejectDocument(@PathVariable UUID docId,
                                                   @Valid @RequestBody RejectRequest req) {
        return ResponseEntity.ok(documentService.reject(docId, req.rejectionReason()));
    }

    @DeleteMapping("/{docId}")
    ResponseEntity<Void> deleteDocument(@PathVariable UUID docId) {
        documentService.delete(docId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{docId}/chunks")
    ResponseEntity<PageResponse<ChunkVM>> getChunks(@PathVariable UUID docId,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(documentService.getChunks(docId, pageable)));
    }
}
