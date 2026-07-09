package com.salessystem.paymentservice.domain.usecase;

import com.salessystem.paymentservice.domain.model.PaymentStatus;
import com.salessystem.paymentservice.domain.model.PaymentTransaction;
import com.salessystem.paymentservice.domain.model.PaymentValidatedEventDTO;
import com.salessystem.paymentservice.ports.outbound.PaymentEventPublisherPort;
import com.salessystem.paymentservice.ports.outbound.PaymentRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentUseCaseTest {

    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;

    @Mock
    private PaymentEventPublisherPort paymentEventPublisherPort;

    @InjectMocks
    private ProcessPaymentUseCase processPaymentUseCase;

    private String key;
    private Integer orderId;
    private Integer customerId;
    private BigDecimal totalAmount;

    @BeforeEach
    void setUp() {
        // Given: Standard parameters used across tests
        key = "kafka-key-123";
        orderId = 1;
        customerId = 100;
        totalAmount = new BigDecimal("150.00");
    }

    // ==========================================
    // TESTS FOR processPayment()
    // ==========================================

    @Test
    @DisplayName("Should approve payment and publish success event when token is valid")
    void shouldProcessPaymentWithSuccess() {
        // Given
        String validToken = "VALID_TOKEN_12345";

        when(paymentRepositoryPort.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepositoryPort.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PaymentTransaction result = processPaymentUseCase.processPayment(key, orderId, customerId, totalAmount, validToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // Verify that the exact event DTO was published to Kafka
        // program a captor, when .publish executes it will capture the dto
        ArgumentCaptor<PaymentValidatedEventDTO> eventCaptor = ArgumentCaptor.forClass(PaymentValidatedEventDTO.class);
        // verify if the event is publish once (times 1) and asserts the key
        verify(paymentEventPublisherPort, times(1)).publish(eventCaptor.capture(), eq(key));

        PaymentValidatedEventDTO publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(orderId);
        assertThat(publishedEvent.getServiceStatus()).isEqualTo("SUCCESS");
        assertThat(publishedEvent.getReason()).isEqualTo("Payment processed and approved!");
    }

    @Test
    @DisplayName("Should decline payment and publish failed event when token ends with 99")
    void shouldDeclinePaymentWhenTokenEndsWith99() {
        // Given
        String declinedToken = "TOKEN_ENDING_IN_99";

        when(paymentRepositoryPort.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepositoryPort.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PaymentTransaction result = processPaymentUseCase.processPayment(key, orderId, customerId, totalAmount, declinedToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);

        ArgumentCaptor<PaymentValidatedEventDTO> eventCaptor = ArgumentCaptor.forClass(PaymentValidatedEventDTO.class);
        verify(paymentEventPublisherPort, times(1)).publish(eventCaptor.capture(), eq(key));

        PaymentValidatedEventDTO publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getServiceStatus()).isEqualTo("FAILED");
        assertThat(publishedEvent.getReason()).isEqualTo("Payment declined..");
    }

    @Test
    @DisplayName("Should return null and log error when payment is duplicated")
    void shouldNotProcessPaymentWhenAlreadyExists() {
        // Given
        String anyToken = "ANY_TOKEN";
        PaymentTransaction existingTransaction = new PaymentTransaction();
        existingTransaction.setOrderId(orderId);

        when(paymentRepositoryPort.findByOrderId(orderId)).thenReturn(Optional.of(existingTransaction));

        // When
        PaymentTransaction result = processPaymentUseCase.processPayment(key, orderId, customerId, totalAmount, anyToken);

        // Then
        assertThat(result).isNull();
        verify(paymentRepositoryPort, never()).save(any());
        verify(paymentEventPublisherPort, never()).publish(any(), any());
    }

    // ==========================================
    // TESTS FOR processRefund()
    // ==========================================

    @Test
    @DisplayName("Should refund payment successfully when original status was SUCCESS")
    void shouldRefundPaymentSuccessfully() {
        // Given
        PaymentTransaction existingSuccessTransaction = new PaymentTransaction();
        existingSuccessTransaction.setOrderId(orderId);
        existingSuccessTransaction.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepositoryPort.findByOrderId(orderId)).thenReturn(Optional.of(existingSuccessTransaction));

        // When
        processPaymentUseCase.processRefund(key, orderId);

        // Then
        assertThat(existingSuccessTransaction.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepositoryPort, times(1)).save(existingSuccessTransaction);

        ArgumentCaptor<PaymentValidatedEventDTO> eventCaptor = ArgumentCaptor.forClass(PaymentValidatedEventDTO.class);
        verify(paymentEventPublisherPort, times(1)).publish(eventCaptor.capture(), eq(key));

        PaymentValidatedEventDTO publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getServiceStatus()).isEqualTo("REFUNDED");
        assertThat(publishedEvent.getReason()).isEqualTo("Payment refunded!");
    }

    @Test
    @DisplayName("Should do nothing and log error when trying to refund an already refunded transaction")
    void shouldNotRefundWhenAlreadyRefunded() {
        // Given
        PaymentTransaction existingRefundedTransaction = new PaymentTransaction();
        existingRefundedTransaction.setOrderId(orderId);
        existingRefundedTransaction.setStatus(PaymentStatus.REFUNDED);

        when(paymentRepositoryPort.findByOrderId(orderId)).thenReturn(Optional.of(existingRefundedTransaction));

        // When
        processPaymentUseCase.processRefund(key, orderId);

        // Then
        verify(paymentRepositoryPort, never()).save(any());
        verify(paymentEventPublisherPort, never()).publish(any(), any());
    }

    @Test
    @DisplayName("Should do nothing and log error when payment is not found for refund")
    void shouldNotProcessRefundWhenPaymentNotFound() {
        // Given: The repository returns an empty Optional (payment does not exist)
        when(paymentRepositoryPort.findByOrderId(orderId)).thenReturn(Optional.empty());

        // When: Attempting to process a refund for a non-existing payment
        processPaymentUseCase.processRefund(key, orderId);

        // Then: Verify that the system never attempts to save or publish an event
        verify(paymentRepositoryPort, never()).save(any());
        verify(paymentEventPublisherPort, never()).publish(any(), any());
    }
}