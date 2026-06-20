package com.lahat.muolana.legaldocuments.web;

import com.lahat.muolana.legaldocuments.domain.*;
import com.lahat.muolana.legaldocuments.web.dtos.RejectRequest;
import com.lahat.muolana.legaldocuments.web.dtos.UploadRequest;
import com.lahat.muolana.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/legal-documents")
@PreAuthorize("hasRole('ADMIN')")
class AdminLegalDocumentController {

    private final LegalDocumentService documentService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    AdminLegalDocumentController(LegalDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<LegalDocumentVM> upload(
            @Valid @RequestPart("metadata") UploadRequest req,
            @RequestPart("file") MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path dest = Paths.get(uploadDir).resolve(filename).normalize();
        Files.createDirectories(dest.getParent());
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        UploadDocumentCmd cmd = new UploadDocumentCmd(
                req.title(), req.shortName(),
                DocumentCategory.valueOf(req.category().toUpperCase()),
                req.versionLabel(), req.sourceUrl(), filename);
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
