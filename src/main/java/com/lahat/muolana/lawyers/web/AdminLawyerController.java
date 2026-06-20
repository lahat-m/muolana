package com.lahat.muolana.lawyers.web;

import com.lahat.muolana.lawyers.domain.*;
import com.lahat.muolana.lawyers.web.dtos.AddContactMethodRequest;
import com.lahat.muolana.lawyers.web.dtos.AddSpecialisationRequest;
import com.lahat.muolana.lawyers.web.dtos.CreateLawyerRequest;
import com.lahat.muolana.lawyers.web.dtos.PatchLawyerRequest;
import com.lahat.muolana.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/v1/admin/lawyers")
@PreAuthorize("hasRole('ADMIN')")
class AdminLawyerController {

    private static final Logger log = LoggerFactory.getLogger(AdminLawyerController.class);

    private final LawyerService lawyerService;

    AdminLawyerController(LawyerService lawyerService) {
        this.lawyerService = lawyerService;
    }

    @PostMapping
    ResponseEntity<LawyerVM> createLawyer(@Valid @RequestBody CreateLawyerRequest createLawyerRequest) {
        log.info("Admin POST /lawyers name='{}' barNumber='{}'", createLawyerRequest.fullName(), createLawyerRequest.barNumber());
        CreateLawyerCmd createLawyerCommand = new CreateLawyerCmd(
                createLawyerRequest.fullName(), createLawyerRequest.barNumber(), createLawyerRequest.lawFirmName(), createLawyerRequest.locationCity(),
                FeeType.valueOf(createLawyerRequest.feeType().toUpperCase()), createLawyerRequest.bio());
        LawyerVM lawyer = lawyerService.createLawyer(createLawyerCommand);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(lawyer.id()).toUri();
        return created(location).body(lawyer);
    }

    @GetMapping
    ResponseEntity<PageResponse<LawyerVM>> listLawyers(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ok(PageResponse.of(lawyerService.adminListLawyers(status, pageable)));
    }

    @GetMapping("/{lawyerId}")
    ResponseEntity<LawyerVM> getLawyer(@PathVariable UUID lawyerId) {
        return ok(lawyerService.adminGetLawyer(lawyerId));
    }

    @PutMapping("/{lawyerId}")
    ResponseEntity<LawyerVM> replaceLawyer(@PathVariable UUID lawyerId,
                                           @Valid @RequestBody CreateLawyerRequest replaceLawyerRequest) {
        log.info("Admin PUT /lawyers/{}", lawyerId);
        CreateLawyerCmd replaceLawyerCommand = new CreateLawyerCmd(
                replaceLawyerRequest.fullName(), replaceLawyerRequest.barNumber(), replaceLawyerRequest.lawFirmName(), replaceLawyerRequest.locationCity(),
                FeeType.valueOf(replaceLawyerRequest.feeType().toUpperCase()), replaceLawyerRequest.bio());
        return ok(lawyerService.replaceLawyer(lawyerId, replaceLawyerCommand));
    }

    @PatchMapping("/{lawyerId}")
    ResponseEntity<LawyerVM> patchLawyer(@PathVariable UUID lawyerId,
                                         @RequestBody PatchLawyerRequest patchLawyerRequest) {
        log.info("Admin PATCH /lawyers/{}", lawyerId);
        FeeType feeType = patchLawyerRequest.feeType() != null ? FeeType.valueOf(patchLawyerRequest.feeType().toUpperCase()) : null;
        UpdateLawyerCmd updateLawyerCommand = new UpdateLawyerCmd(patchLawyerRequest.fullName(), patchLawyerRequest.lawFirmName(), patchLawyerRequest.locationCity(), feeType, patchLawyerRequest.bio());
        return ok(lawyerService.patchLawyer(lawyerId, updateLawyerCommand));
    }

    @PatchMapping("/{lawyerId}/approve")
    ResponseEntity<LawyerVM> approveLawyer(@PathVariable UUID lawyerId) {
        log.info("Admin PATCH /lawyers/{}/approve", lawyerId);
        return ok(lawyerService.approveLawyer(lawyerId));
    }

    @PatchMapping("/{lawyerId}/suspend")
    ResponseEntity<LawyerVM> suspendLawyer(@PathVariable UUID lawyerId) {
        log.info("Admin PATCH /lawyers/{}/suspend", lawyerId);
        return ok(lawyerService.suspendLawyer(lawyerId));
    }

    @DeleteMapping("/{lawyerId}")
    ResponseEntity<Void> deleteLawyer(@PathVariable UUID lawyerId) {
        log.info("Admin DELETE /lawyers/{}", lawyerId);
        lawyerService.deleteLawyer(lawyerId);
        return noContent().build();
    }

    // ── Specialisations ───────────────────────────────────────

    @PostMapping("/{lawyerId}/specialisations")
    ResponseEntity<SpecialisationVM> addSpecialisation(@PathVariable UUID lawyerId,
                                                       @Valid @RequestBody AddSpecialisationRequest addSpecialisationRequest) {
        SpecialisationVM specialisation = lawyerService.addSpecialisation(lawyerId, addSpecialisationRequest.area());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(specialisation.id()).toUri();
        return created(location).body(specialisation);
    }

    @DeleteMapping("/{lawyerId}/specialisations/{id}")
    ResponseEntity<Void> removeSpecialisation(@PathVariable UUID lawyerId, @PathVariable UUID id) {
        lawyerService.removeSpecialisation(lawyerId, id);
        return noContent().build();
    }

    // ── Contact methods ───────────────────────────────────────

    @PostMapping("/{lawyerId}/contact-methods")
    ResponseEntity<ContactMethodVM> addContactMethod(@PathVariable UUID lawyerId,
                                                     @Valid @RequestBody AddContactMethodRequest addContactMethodRequest) {
        ContactChannel channel = ContactChannel.valueOf(addContactMethodRequest.channel().toUpperCase());
        ContactMethodVM contactMethod = lawyerService.addContactMethod(lawyerId, channel, addContactMethodRequest.value(), addContactMethodRequest.isPrimary());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(contactMethod.id()).toUri();
        return created(location).body(contactMethod);
    }

    @DeleteMapping("/{lawyerId}/contact-methods/{id}")
    ResponseEntity<Void> removeContactMethod(@PathVariable UUID lawyerId, @PathVariable UUID id) {
        lawyerService.removeContactMethod(lawyerId, id);
        return noContent().build();
    }
}
