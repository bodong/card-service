package xyz.pakwo.cardservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import xyz.pakwo.cardservice.config.CardServiceProperties;
import xyz.pakwo.cardservice.dto.request.CreateAuthorizationRequest;
import xyz.pakwo.cardservice.dto.request.UpdateAuthorizationStatusRequest;
import xyz.pakwo.cardservice.dto.response.CardAuthorizationResponse;
import xyz.pakwo.cardservice.dto.response.PageResponse;
import xyz.pakwo.cardservice.dto.response.RiskCheckResponse;
import xyz.pakwo.cardservice.entity.AuthorizationStatus;
import xyz.pakwo.cardservice.entity.RiskLevel;
import xyz.pakwo.cardservice.service.AuthorizationRiskService;
import xyz.pakwo.cardservice.service.CardAuthorizationService;
import xyz.pakwo.cardservice.util.SensitiveDataMasker;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author sarwo.wibowo
 **/
@WebMvcTest(CardAuthorizationController.class)
@EnableConfigurationProperties(CardServiceProperties.class)
@TestPropertySource(properties = {
        "card-service.pagination.max-page-size=50",
        "card-service.masking.json-fields[0]=cardNumber",
        "card-service.masking.json-fields[1]=cvv",
        "card-service.masking.json-fields[2]=pin",
        "card-service.masking.json-fields[3]=password",
        "card-service.masking.json-fields[4]=token"
})
@Import({SensitiveDataMasker.class})
class CardAuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardAuthorizationService service;

    @MockitoBean
    private AuthorizationRiskService authorizationRiskService;

    @Test
    void create_shouldReturnCreatedAuthorization() throws Exception {
        CreateAuthorizationRequest request = new CreateAuthorizationRequest("TXN-001",
                "5454545454545454", "CUST-001", "Pakwo Store",
                BigDecimal.valueOf(120.50), "MYR");

        CardAuthorizationResponse response = createAuthorizationResponse(1L, AuthorizationStatus.PENDING, null);

        when(service.create(any(CreateAuthorizationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/authorizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "test-correlation-id")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.transactionReference").value("TXN-001"))
                .andExpect(jsonPath("$.cardNumberMasked").value("545454******5454"))
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.merchantName").value("Pakwo Store"))
                .andExpect(jsonPath("$.amount").value(120.50))
                .andExpect(jsonPath("$.currency").value("MYR"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        ArgumentCaptor<CreateAuthorizationRequest> captor = ArgumentCaptor.forClass(CreateAuthorizationRequest.class);

        verify(service).create(captor.capture());

        assertThat(captor.getValue().transactionReference()).isEqualTo("TXN-001");
        assertThat(captor.getValue().cardNumber()).isEqualTo("5454545454545454");
    }

    @Test
    void getById_shouldReturnAuthorization() throws Exception {
        CardAuthorizationResponse response = createAuthorizationResponse(1L, AuthorizationStatus.PENDING, null);

        when(service.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/authorizations/{id}", 1L)
                        .header("X-Correlation-Id", "test-correlation-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.transactionReference").value("TXN-001"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(service).getById(1L);
    }

    @Test
    void getAll_shouldReturnPagedAuthorizations() throws Exception {
        CardAuthorizationResponse response = createAuthorizationResponse(1L, AuthorizationStatus.PENDING, null);

        PageResponse<CardAuthorizationResponse> pageResponse = new PageResponse<>(
                List.of(response),
                0,
                10,
                1,
                1,
                true,
                true
        );

        when(service.getAll(any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/authorizations")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .header("X-Correlation-Id", "test-correlation-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].transactionReference").value("TXN-001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).getAll(pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void updateStatus_shouldReturnUpdatedAuthorization() throws Exception {
        UpdateAuthorizationStatusRequest request = new UpdateAuthorizationStatusRequest(AuthorizationStatus.APPROVED);

        CardAuthorizationResponse response = createAuthorizationResponse(1L, AuthorizationStatus.APPROVED, RiskLevel.LOW);

        when(service.updateStatus(eq(1L), any(UpdateAuthorizationStatusRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/authorizations/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "test-correlation-id")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.riskLevel").value("LOW"));

        ArgumentCaptor<UpdateAuthorizationStatusRequest> captor = ArgumentCaptor.forClass(UpdateAuthorizationStatusRequest.class);
        verify(service).updateStatus(eq(1L), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(AuthorizationStatus.APPROVED);
    }

    @Test
    void performRiskCheck_shouldReturnRiskCheckResponse() throws Exception {
        RiskCheckResponse response = new RiskCheckResponse(1L, "TXN-001", 25,
                RiskLevel.LOW, "APPROVED", "POST-1");

        when(authorizationRiskService.performRiskCheck(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/authorizations/{id}/risk-check", 1L)
                        .header("X-Correlation-Id", "test-correlation-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationId").value(1))
                .andExpect(jsonPath("$.transactionReference").value("TXN-001"))
                .andExpect(jsonPath("$.riskScore").value(25))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.recommendation").value("APPROVED"))
                .andExpect(jsonPath("$.externalReference").value("POST-1"));

        verify(authorizationRiskService).performRiskCheck(1L);
    }

    @Test
    void create_shouldReturnBadRequest_whenPayloadIsInvalid() throws Exception {
        String invalidRequest = """
                {
                  "transactionReference": "",
                  "cardNumber": "",
                  "customerId": "",
                  "merchantName": "",
                  "amount": 0,
                  "currency": "MY"
                }
                """;

        mockMvc.perform(post("/api/v1/authorizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "test-correlation-id")
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_shouldReturnBadRequest_whenStatusIsInvalidEnum() throws Exception {
        String invalidRequest = """
                {
                  "status": "FAKE"
                }
                """;

        mockMvc.perform(put("/api/v1/authorizations/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "test-correlation-id")
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    private CardAuthorizationResponse createAuthorizationResponse(Long id, AuthorizationStatus status, RiskLevel riskLevel) {
        return new CardAuthorizationResponse(
                id,
                "TXN-001",
                "545454******5454",
                "CUST-001",
                "Pakwo Store",
                BigDecimal.valueOf(120.50),
                "MYR",
                status,
                null,
                riskLevel,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}