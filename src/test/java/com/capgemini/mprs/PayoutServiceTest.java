package com.capgemini.mprs;

import com.capgemini.mprs.entities.Payout;
import com.capgemini.mprs.repositories.PayoutRepository;
import com.capgemini.mprs.services.PayoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock
    private PayoutRepository payoutRepository;

    @InjectMocks
    private PayoutService payoutService;

    @Test
    @DisplayName("ingestChunk: delegates to repository.saveAll with the same list")
    void ingestChunk_callsSaveAll() {
        // Prepare a small chunk of payouts that should be passed through unchanged.
        Payout p1 = new Payout();
        p1.setTransactionId("T1");
        p1.setPayoutAmount(new BigDecimal("10.00"));
        p1.setPayoutDate(LocalDate.of(2024, 1, 1));

        Payout p2 = new Payout();
        p2.setTransactionId("T2");
        p2.setPayoutAmount(new BigDecimal("20.00"));
        p2.setPayoutDate(LocalDate.of(2024, 1, 2));

        List<Payout> chunk = Arrays.asList(p1, p2);

        // Execute the service call under test.
        payoutService.ingestChunk(chunk);

        // Capture the argument sent to repository.saveAll to assert identity and contents.
        ArgumentCaptor<List<Payout>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(payoutRepository, times(1)).saveAll(captor.capture());

        List<Payout> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        // Ensure the same objects are forwarded (no defensive copy/mutation in the service).
        assertThat(saved.get(0)).isSameAs(p1);
        assertThat(saved.get(1)).isSameAs(p2);

        verifyNoMoreInteractions(payoutRepository);
    }

    @Test
    @DisplayName("findAllPayouts: returns the list from repository.findAll")
    void findAllPayouts_returnsRepositoryResults() {
        // Arrange a repository response.
        Payout p1 = new Payout();
        p1.setTransactionId("A1");
        p1.setPayoutAmount(new BigDecimal("15.50"));
        p1.setPayoutDate(LocalDate.of(2024, 3, 10));

        Payout p2 = new Payout();
        p2.setTransactionId("A2");
        p2.setPayoutAmount(new BigDecimal("25.00"));
        p2.setPayoutDate(LocalDate.of(2024, 3, 11));

        List<Payout> repoList = Arrays.asList(p1, p2);
        when(payoutRepository.findAll()).thenReturn(repoList);

        // Call the service.
        List<Payout> result = payoutService.findAllPayouts();

        // Assert the service simply returns what the repository provided.
        assertThat(result).containsExactly(p1, p2);

        verify(payoutRepository, times(1)).findAll();
        verifyNoMoreInteractions(payoutRepository);
    }

    @Test
    @DisplayName("findPayoutById: returns Optional from repository.findById when present")
    void findPayoutById_present() {
        // Arrange a matching entity in the repository.
        Payout p = new Payout();
        p.setTransactionId("XYZ-123");
        p.setPayoutAmount(new BigDecimal("33.33"));
        p.setPayoutDate(LocalDate.of(2024, 2, 29));

        when(payoutRepository.findById("XYZ-123")).thenReturn(Optional.of(p));

        // Call the service and assert.
        Optional<Payout> result = payoutService.findPayoutById("XYZ-123");
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(p);

        verify(payoutRepository, times(1)).findById("XYZ-123");
        verifyNoMoreInteractions(payoutRepository);
    }

    @Test
    @DisplayName("findPayoutById: returns Optional.empty when repository has no match")
    void findPayoutById_empty() {
        when(payoutRepository.findById("MISSING")).thenReturn(Optional.empty());

        Optional<Payout> result = payoutService.findPayoutById("MISSING");
        assertThat(result).isEmpty();

        verify(payoutRepository, times(1)).findById("MISSING");
        verifyNoMoreInteractions(payoutRepository);
    }
}