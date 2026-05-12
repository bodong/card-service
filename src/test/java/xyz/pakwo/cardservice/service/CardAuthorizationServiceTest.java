package xyz.pakwo.cardservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import xyz.pakwo.cardservice.config.CardServiceProperties;
import xyz.pakwo.cardservice.dto.request.CreateAuthorizationRequest;
import xyz.pakwo.cardservice.dto.request.UpdateAuthorizationStatusRequest;
import xyz.pakwo.cardservice.dto.response.CardAuthorizationResponse;
import xyz.pakwo.cardservice.dto.response.PageResponse;
import xyz.pakwo.cardservice.entity.AuthorizationStatus;
import xyz.pakwo.cardservice.entity.CardAuthorization;
import xyz.pakwo.cardservice.entity.RiskLevel;
import xyz.pakwo.cardservice.exception.BadRequestException;
import xyz.pakwo.cardservice.exception.DuplicateTransactionReferenceException;
import xyz.pakwo.cardservice.exception.ResourceNotFoundException;
import xyz.pakwo.cardservice.mapper.CardAuthorizationMapper;
import xyz.pakwo.cardservice.repository.CardAuthorizationRepository;
import xyz.pakwo.cardservice.util.SensitiveDataMasker;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * @author sarwo.wibowo
 **/
@ExtendWith(MockitoExtension.class)
class CardAuthorizationServiceTest {

    @Mock
    private CardAuthorizationRepository repository;
    @Mock
    private CardAuthorizationMapper mapper;
    @Mock
    private SensitiveDataMasker sensitiveDataMasker;
    private CardAuthorizationService service;

    @BeforeEach
    void setUp() {
        CardServiceProperties properties = new CardServiceProperties(
                new CardServiceProperties.Pagination(50),
                new CardServiceProperties.Masking(List.of("cardNumber", "cvv", "pin", "password", "token"))
        );

        service = new CardAuthorizationService(repository, mapper, sensitiveDataMasker, properties);
    }

    @Test
    void create_shouldCreateAuthorization_whenTransactionReferenceIsUnique() {
        CreateAuthorizationRequest request = createRequest();

        CardAuthorization entity = createPendingEntity();
        CardAuthorization savedEntity = createPendingEntity();
        savedEntity.setId(1L);

        CardAuthorizationResponse response = createResponse(1L, AuthorizationStatus.PENDING, null);

        when(repository.existsByTransactionReference("TXN-001")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(sensitiveDataMasker.maskCardNumber("5454545454545454")).thenReturn("545454******5454");
        when(repository.saveAndFlush(entity)).thenReturn(savedEntity);
        when(mapper.toResponse(savedEntity)).thenReturn(response);

        CardAuthorizationResponse result = service.create(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.transactionReference()).isEqualTo("TXN-001");
        assertThat(result.status()).isEqualTo(AuthorizationStatus.PENDING);

        verify(repository).existsByTransactionReference("TXN-001");
        verify(repository).saveAndFlush(entity);

        assertThat(entity.getTransactionReference()).isEqualTo("TXN-001");
        assertThat(entity.getCardNumberMasked()).isEqualTo("545454******5454");
        assertThat(entity.getStatus()).isEqualTo(AuthorizationStatus.PENDING);
    }

    @Test
    void create_shouldThrowConflict_whenTransactionReferenceAlreadyExists() {
        CreateAuthorizationRequest request = createRequest();

        when(repository.existsByTransactionReference("TXN-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateTransactionReferenceException.class)
                .hasMessageContaining("TXN-001");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void create_shouldThrowConflict_whenDatabaseUniqueConstraintIsViolated() {
        CreateAuthorizationRequest request = createRequest();

        CardAuthorization entity = createPendingEntity();

        when(repository.existsByTransactionReference("TXN-001")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(sensitiveDataMasker.maskCardNumber("5454545454545454")).thenReturn("545454******5454");
        when(repository.saveAndFlush(entity)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateTransactionReferenceException.class)
                .hasMessageContaining("TXN-001");
    }

    @Test
    void getById_shouldReturnAuthorization_whenFound() {
        CardAuthorization entity = createPendingEntity();
        entity.setId(1L);

        CardAuthorizationResponse response = createResponse(1L, AuthorizationStatus.PENDING, null);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        CardAuthorizationResponse result = service.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(AuthorizationStatus.PENDING);
    }

    @Test
    void getById_shouldThrowNotFound_whenAuthorizationDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Card authorization not found: 999");
    }

    @Test
    void getAll_shouldReturnPagedAuthorizations_whenPageSizeIsAllowed() {
        CardAuthorization entity = createPendingEntity();
        entity.setId(1L);

        CardAuthorizationResponse response = createResponse(1L, AuthorizationStatus.PENDING, null);
        PageRequest pageable = PageRequest.of(0, 10);

        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toResponse(entity)).thenReturn(response);

        PageResponse<CardAuthorizationResponse> result = service.getAll(pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
    }

    @Test
    void getAll_shouldThrowBadRequest_whenPageSizeExceedsConfiguredLimit() {
        PageRequest pageable = PageRequest.of(0, 100);

        assertThatThrownBy(() -> service.getAll(pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page size must not exceed 50");

        verify(repository, never()).findAll(any(PageRequest.class));
    }

    @Test
    void updateStatus_shouldApprove_whenRiskCheckCompletedAndRiskIsLow() {
        CardAuthorization entity = createPendingEntity();
        entity.setId(1L);
        entity.setRiskLevel(RiskLevel.LOW);

        CardAuthorizationResponse response = createResponse(1L, AuthorizationStatus.APPROVED, RiskLevel.LOW);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        CardAuthorizationResponse result = service.updateStatus(1L, new UpdateAuthorizationStatusRequest(AuthorizationStatus.APPROVED));

        assertThat(entity.getStatus()).isEqualTo(AuthorizationStatus.APPROVED);
        assertThat(result.status()).isEqualTo(AuthorizationStatus.APPROVED);
    }

    @Test
    void updateStatus_shouldAllowDeclinedWithoutRiskCheck() {
        CardAuthorization entity = createPendingEntity();
        entity.setId(1L);
        entity.setRiskLevel(null);

        CardAuthorizationResponse response = createResponse(1L, AuthorizationStatus.DECLINED, null);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        CardAuthorizationResponse result = service.updateStatus(1L, new UpdateAuthorizationStatusRequest(AuthorizationStatus.DECLINED));

        assertThat(entity.getStatus()).isEqualTo(AuthorizationStatus.DECLINED);
        assertThat(result.status()).isEqualTo(AuthorizationStatus.DECLINED);
    }

    @Test
    void updateStatus_shouldRejectApproval_whenRiskCheckNotCompleted() {
        CardAuthorization entity = createPendingEntity();
        entity.setId(1L);
        entity.setRiskLevel(null);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus(1L, new UpdateAuthorizationStatusRequest(AuthorizationStatus.APPROVED)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Risk check must be completed before authorization can be approved");

        assertThat(entity.getStatus()).isEqualTo(AuthorizationStatus.PENDING);
    }

    @Test
    void updateStatus_shouldRejectApproval_whenRiskLevelIsHigh() {
        CardAuthorization entity = createPendingEntity();
        entity.setId(1L);
        entity.setRiskLevel(RiskLevel.HIGH);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus(
                1L,
                new UpdateAuthorizationStatusRequest(AuthorizationStatus.APPROVED)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("High risk authorization cannot be approved");

        assertThat(entity.getStatus()).isEqualTo(AuthorizationStatus.PENDING);
    }

    @Test
    void updateStatus_shouldRejectSameStatus() {
        CardAuthorization entity = createPendingEntity();
        entity.setId(1L);
        entity.setStatus(AuthorizationStatus.PENDING);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus(1L, new UpdateAuthorizationStatusRequest(AuthorizationStatus.PENDING)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Authorization is already in status PENDING");
    }

    @Test
    void updateStatus_shouldRejectChangeAfterApproved() {
        CardAuthorization entity = createPendingEntity();
        entity.setId(1L);
        entity.setStatus(AuthorizationStatus.APPROVED);
        entity.setRiskLevel(RiskLevel.LOW);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus(1L, new UpdateAuthorizationStatusRequest(AuthorizationStatus.DECLINED)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Authorization status cannot be changed after it is APPROVED");
    }

    @Test
    void updateStatus_shouldRejectChangeAfterDeclined() {
        CardAuthorization entity = createPendingEntity();
        entity.setId(1L);
        entity.setStatus(AuthorizationStatus.DECLINED);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus(1L, new UpdateAuthorizationStatusRequest(AuthorizationStatus.FAILED)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Authorization status cannot be changed after it is DECLINED");
    }

    @Test
    void updateStatus_shouldRejectChangeAfterFailed() {
        CardAuthorization entity = createPendingEntity();
        entity.setId(1L);
        entity.setStatus(AuthorizationStatus.FAILED);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus(1L, new UpdateAuthorizationStatusRequest(AuthorizationStatus.DECLINED)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Authorization status cannot be changed after it is FAILED");
    }

    private CreateAuthorizationRequest createRequest() {
        return new CreateAuthorizationRequest("TXN-001", "5454545454545454", "CUST-001",
                "Pakwo Store", BigDecimal.valueOf(120.50), "MYR");
    }

    private CardAuthorization createPendingEntity() {
        CardAuthorization entity = new CardAuthorization();
        entity.setTransactionReference("TXN-001");
        entity.setCardNumberMasked("545454******5454");
        entity.setCustomerId("CUST-001");
        entity.setMerchantName("Pakwo Store");
        entity.setAmount(BigDecimal.valueOf(120.50));
        entity.setCurrency("MYR");
        entity.setStatus(AuthorizationStatus.PENDING);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private CardAuthorizationResponse createResponse(Long id, AuthorizationStatus status, RiskLevel riskLevel) {
        return new CardAuthorizationResponse(id, "TXN-001", "545454******5454", "CUST-001",
                "Pakwo Store", BigDecimal.valueOf(120.50), "MYR", status, null, riskLevel,
                null, LocalDateTime.now(), LocalDateTime.now());
    }
}