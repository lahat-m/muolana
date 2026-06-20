package com.lahat.muolana.lawyers.web;

import com.lahat.muolana.lawyers.domain.*;
import com.lahat.muolana.lawyers.web.dtos.CreateReferralRequest;
import com.lahat.muolana.lawyers.web.dtos.CreateReviewRequest;
import com.lahat.muolana.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lawyers")
class LawyerController {

    private static final Logger log = LoggerFactory.getLogger(LawyerController.class);

    private final LawyerService lawyerService;

    LawyerController(LawyerService lawyerService) {
        this.lawyerService = lawyerService;
    }

    @GetMapping
    ResponseEntity<PageResponse<LawyerVM>> browseLawyers(
            @RequestParam(required = false) String city,
            @PageableDefault(size = 100) Pageable pageable) {
        Page<LawyerVM> page = lawyerService.browseLawyers(city, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/{lawyerId}")
    ResponseEntity<LawyerVM> getLawyerProfile(@PathVariable UUID lawyerId) {
        return ResponseEntity.ok(lawyerService.getLawyerProfile(lawyerId));
    }

    @GetMapping("/{lawyerId}/reviews")
    ResponseEntity<PageResponse<ReviewVM>> getLawyerReviews(
            @PathVariable UUID lawyerId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReviewVM> page = lawyerService.getLawyerReviews(lawyerId, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/{lawyerId}/specialisations")
    ResponseEntity<List<SpecialisationVM>> getLawyerSpecialisations(@PathVariable UUID lawyerId) {
        return ResponseEntity.ok(lawyerService.getLawyerSpecialisations(lawyerId));
    }

    @PostMapping("/{lawyerId}/referrals")
    @PreAuthorize("hasRole('CITIZEN')")
    ResponseEntity<ReferralVM> createReferral(
            @PathVariable UUID lawyerId,
            @Valid @RequestBody CreateReferralRequest createReferralRequest,
            @AuthenticationPrincipal Jwt jwt) {
        UUID citizenUserId = UUID.fromString(jwt.getClaimAsString("userId"));
        log.info("POST /lawyers/{}/referrals citizenId={} channel={}", lawyerId, citizenUserId, createReferralRequest.channel());
        ReferralVM referral = lawyerService.createReferral(
                lawyerId, citizenUserId, createReferralRequest.sessionId(),
                ContactChannel.valueOf(createReferralRequest.channel().toUpperCase()), createReferralRequest.consentGiven());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(referral.id()).toUri();
        return ResponseEntity.created(location).body(referral);
    }

    @PostMapping("/{lawyerId}/reviews")
    @PreAuthorize("hasRole('CITIZEN')")
    ResponseEntity<ReviewVM> createReview(
            @PathVariable UUID lawyerId,
            @Valid @RequestBody CreateReviewRequest createReviewRequest,
            @AuthenticationPrincipal Jwt jwt) {
        UUID citizenUserId = UUID.fromString(jwt.getClaimAsString("userId"));
        log.info("POST /lawyers/{}/reviews citizenId={} rating={}", lawyerId, citizenUserId, createReviewRequest.rating());
        ReviewVM review = lawyerService.createReview(
                lawyerId, citizenUserId, createReviewRequest.sessionId(),
                createReviewRequest.rating(), createReviewRequest.reviewText(), createReviewRequest.isAnonymous());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/lawyers/{lawyerId}/reviews/{id}")
                .buildAndExpand(lawyerId, review.id()).toUri();
        return ResponseEntity.created(location).body(review);
    }

    @GetMapping("/api/v1/sessions/{sessionId}/referrals")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<List<ReferralVM>> getReferralsBySession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(lawyerService.getReferralsBySession(sessionId));
    }
}
