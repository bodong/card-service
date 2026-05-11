package xyz.pakwo.cardservice.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.pakwo.cardservice.dto.CardAuthorizationResponse;
import xyz.pakwo.cardservice.dto.CreateAuthorizationRequest;
import xyz.pakwo.cardservice.dto.PageResponse;
import xyz.pakwo.cardservice.dto.UpdateAuthorizationStatusRequest;
import xyz.pakwo.cardservice.entity.AuthorizationStatus;
import xyz.pakwo.cardservice.entity.CardAuthorization;
import xyz.pakwo.cardservice.entity.RiskLevel;
import xyz.pakwo.cardservice.exception.BadRequestException;
import xyz.pakwo.cardservice.exception.DuplicateTransactionReferenceException;
import xyz.pakwo.cardservice.exception.ResourceNotFoundException;
import xyz.pakwo.cardservice.mapper.CardAuthorizationMapper;
import xyz.pakwo.cardservice.repository.CardAuthorizationRepository;
import xyz.pakwo.cardservice.util.SensitiveDataMasker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author sarwo.wibowo
 **/
@Service
public class CardAuthorizationService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter REFERENCE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final CardAuthorizationRepository repository;
    private final CardAuthorizationMapper mapper;
    private final SensitiveDataMasker sensitiveDataMasker;

    public CardAuthorizationService(CardAuthorizationRepository repository, CardAuthorizationMapper mapper,
                                    SensitiveDataMasker sensitiveDataMasker) {
        this.repository = repository;
        this.mapper = mapper;
        this.sensitiveDataMasker = sensitiveDataMasker;
    }

    @Transactional
    public CardAuthorizationResponse create(CreateAuthorizationRequest request) {
        CardAuthorization entity = mapper.toEntity(request);
        entity.setCardNumberMasked(sensitiveDataMasker.maskCardNumber(request.cardNumber()));
        entity.setTransactionReference(generateTransactionReference());
        entity.setStatus(AuthorizationStatus.PENDING);

        try {
            CardAuthorization saved = repository.saveAndFlush(entity);
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateTransactionReferenceException("Generated transaction reference already exists. Please retry.");
        }
    }

    @Transactional(readOnly = true)
    public CardAuthorizationResponse getById(Long id) {
        return mapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<CardAuthorizationResponse> getAll(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must not exceed " + MAX_PAGE_SIZE);
        }

        Page<CardAuthorizationResponse> result = repository.findAll(pageable)
                .map(mapper::toResponse);
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    @Transactional(readOnly = true)
    public CardAuthorization findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card authorization not found: " + id));
    }

    @Transactional
    public CardAuthorizationResponse updateStatus(Long id, UpdateAuthorizationStatusRequest request) {
        CardAuthorization entity = findById(id);
        validateStatusTransition(entity.getStatus(), entity.getRiskLevel(), request.status());
        entity.setStatus(request.status());
        return mapper.toResponse(entity);
    }

    private void validateStatusTransition(AuthorizationStatus currentStatus, RiskLevel riskLevel, AuthorizationStatus requestStatus) {
        if (currentStatus == requestStatus) {
            throw new BadRequestException("Authorization is already in status " + requestStatus);
        }

        if (isTerminalStatus(currentStatus)) {
            throw new BadRequestException("Authorization status cannot be changed after it is " + currentStatus);
        }

        if (riskLevel == null) {
            throw new BadRequestException("Risk check must be completed before authorization can be approved");
        }

        if (AuthorizationStatus.APPROVED == requestStatus && RiskLevel.HIGH == riskLevel) {
            throw new BadRequestException("High risk authorization cannot be approved");
        }
    }

    private boolean isTerminalStatus(AuthorizationStatus status) {
        return status == AuthorizationStatus.APPROVED
                || status == AuthorizationStatus.DECLINED
                || status == AuthorizationStatus.FAILED;
    }

    private String generateTransactionReference() {
        String date = LocalDate.now().format(REFERENCE_DATE_FORMAT);
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "AUTH-" + date + "-" + random;
    }
}
