package com.lahat.muolana.lawyers.domain;

import com.lahat.muolana.shared.exceptions.ConflictException;
import com.lahat.muolana.shared.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LawyerService {

    private static final Logger log = LoggerFactory.getLogger(LawyerService.class);

    private final LawyerRepository lawyerRepository;
    private final SpecialisationRepository specialisationRepository;
    private final ContactMethodRepository contactMethodRepository;
    private final ReviewRepository reviewRepository;
    private final ReferralRepository referralRepository;

    public LawyerService(LawyerRepository lawyerRepository,
                         SpecialisationRepository specialisationRepository,
                         ContactMethodRepository contactMethodRepository,
                         ReviewRepository reviewRepository,
                         ReferralRepository referralRepository) {
        this.lawyerRepository = lawyerRepository;
        this.specialisationRepository = specialisationRepository;
        this.contactMethodRepository = contactMethodRepository;
        this.reviewRepository = reviewRepository;
        this.referralRepository = referralRepository;
    }

    // ── Citizen read ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<LawyerVM> browseLawyers(String city, Pageable pageable) {
        Page<LawyerEntity> page = (city != null && !city.isBlank())
                ? lawyerRepository.findByStatusAndLocationCityContainingIgnoreCase(LawyerStatus.ACTIVE, city, pageable)
                : lawyerRepository.findByStatus(LawyerStatus.ACTIVE, pageable);
        return page.map(this::toVM);
    }

    @Transactional(readOnly = true)
    public LawyerVM getLawyerProfile(UUID lawyerId) {
        LawyerEntity lawyer = lawyerRepository.getById(lawyerId);
        if (lawyer.getStatus() != LawyerStatus.ACTIVE) {
            throw new ResourceNotFoundException("Lawyer " + lawyerId + " not found");
        }
        return toVM(lawyer);
    }

    @Transactional(readOnly = true)
    public Page<ReviewVM> getLawyerReviews(UUID lawyerId, Pageable pageable) {
        lawyerRepository.getById(lawyerId);
        return reviewRepository.findByLawyerId(lawyerId, pageable).map(this::toReviewVM);
    }

    @Transactional(readOnly = true)
    public List<SpecialisationVM> getLawyerSpecialisations(UUID lawyerId) {
        lawyerRepository.getById(lawyerId);
        return specialisationRepository.findByLawyerId(lawyerId).stream().map(this::toSpecVM).toList();
    }

    public ReferralVM createReferral(UUID lawyerId, UUID citizenUserId, UUID sessionId, ContactChannel channel, boolean consentGiven) {
        log.info("Referral created: lawyerId={} citizenId={} channel={} consent={}", lawyerId, citizenUserId, channel, consentGiven);
        lawyerRepository.getById(lawyerId);
        ReferralEntity referral = new ReferralEntity(lawyerId, citizenUserId, sessionId, channel, consentGiven);
        return toReferralVM(referralRepository.save(referral));
    }

    @Transactional(readOnly = true)
    public List<ReferralVM> getReferralsBySession(UUID sessionId) {
        return referralRepository.findBySessionId(sessionId).stream().map(this::toReferralVM).toList();
    }

    public ReviewVM createReview(UUID lawyerId, UUID citizenUserId, UUID sessionId, int rating, String reviewText, boolean isAnonymous) {
        log.info("Review submitted: lawyerId={} citizenId={} rating={} anonymous={}", lawyerId, citizenUserId, rating, isAnonymous);
        LawyerEntity lawyer = lawyerRepository.getById(lawyerId);
        ReviewEntity review = new ReviewEntity(lawyerId, citizenUserId, sessionId, rating, reviewText, isAnonymous);
        reviewRepository.save(review);
        lawyer.addReview(rating);
        lawyerRepository.save(lawyer);
        log.debug("Lawyer averageRating updated: id={} newAvg={} reviewCount={}", lawyerId, lawyer.getAverageRating(), lawyer.getReviewCount());
        return toReviewVM(review);
    }

    // ── Admin CRUD ────────────────────────────────────────────

    public LawyerVM createLawyer(CreateLawyerCmd cmd) {
        log.info("Creating lawyer: name='{}' barNumber='{}'", cmd.fullName(), cmd.barNumber());
        if (lawyerRepository.existsByBarNumber(cmd.barNumber())) {
            log.warn("Bar number '{}' already registered", cmd.barNumber());
            throw new ConflictException("Bar number " + cmd.barNumber() + " is already registered");
        }
        LawyerEntity lawyer = new LawyerEntity(cmd.fullName(), cmd.barNumber(), cmd.locationCity(), cmd.feeType());
        lawyer.updateProfile(null, null, null, cmd.bio(), cmd.lawFirmName());
        LawyerVM lawyerVm = toVM(lawyerRepository.save(lawyer));
        log.info("Lawyer created: id={} name='{}'", lawyerVm.id(), lawyerVm.fullName());
        return lawyerVm;
    }

    @Transactional(readOnly = true)
    public Page<LawyerVM> adminListLawyers(String status, Pageable pageable) {
        if (status != null) {
            LawyerStatus lawyerStatus = LawyerStatus.valueOf(status.toUpperCase());
            return lawyerRepository.findByStatus(lawyerStatus, pageable).map(this::toVM);
        }
        return lawyerRepository.findAll(pageable).map(this::toVM);
    }

    @Transactional(readOnly = true)
    public LawyerVM adminGetLawyer(UUID lawyerId) {
        return toVM(lawyerRepository.getById(lawyerId));
    }

    public LawyerVM replaceLawyer(UUID lawyerId, CreateLawyerCmd cmd) {
        log.info("Replacing lawyer profile: id={}", lawyerId);
        LawyerEntity lawyer = lawyerRepository.getById(lawyerId);
        lawyer.updateProfile(cmd.fullName(), cmd.locationCity(), cmd.feeType(), cmd.bio(), cmd.lawFirmName());
        return toVM(lawyerRepository.save(lawyer));
    }

    public LawyerVM patchLawyer(UUID lawyerId, UpdateLawyerCmd cmd) {
        log.info("Patching lawyer profile: id={}", lawyerId);
        LawyerEntity lawyer = lawyerRepository.getById(lawyerId);
        lawyer.updateProfile(cmd.fullName(), cmd.locationCity(), cmd.feeType(), cmd.bio(), cmd.lawFirmName());
        return toVM(lawyerRepository.save(lawyer));
    }

    public LawyerVM approveLawyer(UUID lawyerId) {
        log.info("Approving lawyer: id={}", lawyerId);
        LawyerEntity lawyer = lawyerRepository.getById(lawyerId);
        lawyer.approve();
        LawyerVM lawyerVm = toVM(lawyerRepository.save(lawyer));
        log.info("Lawyer approved and set ACTIVE: id={} name='{}'", lawyerId, lawyerVm.fullName());
        return lawyerVm;
    }

    public LawyerVM suspendLawyer(UUID lawyerId) {
        log.info("Suspending lawyer: id={}", lawyerId);
        LawyerEntity lawyer = lawyerRepository.getById(lawyerId);
        lawyer.suspend();
        LawyerVM lawyerVm = toVM(lawyerRepository.save(lawyer));
        log.info("Lawyer suspended: id={} name='{}'", lawyerId, lawyerVm.fullName());
        return lawyerVm;
    }

    public void deleteLawyer(UUID lawyerId) {
        log.info("Deleting lawyer: id={}", lawyerId);
        LawyerEntity lawyer = lawyerRepository.getById(lawyerId);
        lawyerRepository.delete(lawyer);
        log.info("Lawyer deleted: id={} name='{}'", lawyerId, lawyer.getFullName());
    }

    // ── Sub-resources (admin) ─────────────────────────────────

    public SpecialisationVM addSpecialisation(UUID lawyerId, String area) {
        log.debug("Adding specialisation '{}' to lawyer id={}", area, lawyerId);
        lawyerRepository.getById(lawyerId);
        return toSpecVM(specialisationRepository.save(new SpecialisationEntity(lawyerId, area)));
    }

    public void removeSpecialisation(UUID lawyerId, UUID specId) {
        log.debug("Removing specialisation id={} from lawyer id={}", specId, lawyerId);
        SpecialisationEntity spec = specialisationRepository.getByIdAndLawyerId(specId, lawyerId);
        specialisationRepository.delete(spec);
    }

    public ContactMethodVM addContactMethod(UUID lawyerId, ContactChannel channel, String value, boolean isPrimary) {
        log.debug("Adding contact method channel={} to lawyer id={}", channel, lawyerId);
        lawyerRepository.getById(lawyerId);
        return toContactVM(contactMethodRepository.save(new ContactMethodEntity(lawyerId, channel, value, isPrimary)));
    }

    public void removeContactMethod(UUID lawyerId, UUID contactId) {
        log.debug("Removing contact method id={} from lawyer id={}", contactId, lawyerId);
        ContactMethodEntity contact = contactMethodRepository.getByIdAndLawyerId(contactId, lawyerId);
        contactMethodRepository.delete(contact);
    }

    public long getReferralCount(UUID lawyerId) {
        return referralRepository.countByLawyerId(lawyerId);
    }

    // ── Mappers ───────────────────────────────────────────────

    private LawyerVM toVM(LawyerEntity lawyer) {
        List<String> specs = specialisationRepository.findByLawyerId(lawyer.getId())
                .stream().map(SpecialisationEntity::getArea).toList();
        List<ContactMethodVM> contacts = contactMethodRepository.findByLawyerId(lawyer.getId())
                .stream().map(this::toContactVM).toList();
        return new LawyerVM(lawyer.getId(), lawyer.getFullName(), lawyer.getBarNumber(), lawyer.getLawFirmName(),
                lawyer.getLocationCity(), lawyer.getFeeType().name(), lawyer.getBio(), lawyer.isVerified(), lawyer.getStatus().name(),
                lawyer.getAverageRating(), lawyer.getReviewCount(), lawyer.getCreatedAt(), specs, contacts);
    }

    private SpecialisationVM toSpecVM(SpecialisationEntity specialisation) {
        return new SpecialisationVM(specialisation.getId(), specialisation.getArea());
    }

    private ContactMethodVM toContactVM(ContactMethodEntity contactMethod) {
        return new ContactMethodVM(contactMethod.getId(), contactMethod.getChannel().name(), contactMethod.getValue(), contactMethod.isPrimary());
    }

    private ReviewVM toReviewVM(ReviewEntity review) {
        return new ReviewVM(review.getId(), review.getRating(), review.getReviewText(), review.isAnonymous(), review.getCreatedAt());
    }

    private ReferralVM toReferralVM(ReferralEntity referral) {
        return new ReferralVM(referral.getId(), referral.getLawyerId(), referral.getStatus(), referral.getCreatedAt());
    }
}
