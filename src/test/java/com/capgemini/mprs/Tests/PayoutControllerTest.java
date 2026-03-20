package com.capgemini.mprs.Tests;

import com.capgemini.mprs.controllers.PayoutController;
import com.capgemini.mprs.entities.Payout;
import com.capgemini.mprs.services.PayoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
        import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-slice tests for PayoutController using MockMvc.
 * PayoutService is mocked so we only verify controller behavior (parsing, validation, chunking, and mapping).
 */

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(PayoutController.class)
class PayoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PayoutService payoutService;

    @Test
    @DisplayName("POST /api/v1/payouts/bulk - parses valid rows and calls ingestChunk once")
    void ingest_validCsv_parsesTwoRows_andCallsServiceOnce_andReturnsOk() throws Exception {
        // The controller supports two date formats: yyyy-MM-dd and dd-MM-yyyy.
        // This CSV includes both formats and should produce two valid Payout objects.
        String csv = String.join("\n",
                "transactionId,payoutAmount,payoutDate",  // header (skipped by controller)
                "TXN-1,12.34,2024-01-15",                 // ISO date
                "TXN-2,-5.67,15-02-2024"                  // dd-MM-yyyy date; negative amount is allowed
        );

        // Multipart file param name must be "file" to match @RequestParam MultipartFile file.
        MockMultipartFile file = new MockMultipartFile(
                "file", "payouts.csv", "text/csv", csv.getBytes()
        );

        // Execute request and assert HTTP 200 with controller's success message.
        mockMvc.perform(multipart("/api/v1/payouts/bulk").file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().string("CSV ingest complete"));

        // Capture payload sent to service to assert correct parsing and values.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Payout>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(payoutService, times(1)).ingestChunk(captor.capture());
        List<Payout> ingested = captor.getValue();

        // Expect two valid rows
        assertThat(ingested).hasSize(2);

        // Row 1 assertions
        assertThat(ingested.get(0).getTransactionId()).isEqualTo("TXN-1");
        assertThat(ingested.get(0).getPayoutAmount()).isEqualByComparingTo(new BigDecimal("12.34"));
        assertThat(ingested.get(0).getPayoutDate()).isEqualTo(LocalDate.parse("2024-01-15"));

        // Row 2 assertions
        assertThat(ingested.get(1).getTransactionId()).isEqualTo("TXN-2");
        assertThat(ingested.get(1).getPayoutAmount()).isEqualByComparingTo(new BigDecimal("-5.67"));
        assertThat(ingested.get(1).getPayoutDate()).isEqualTo(LocalDate.of(2024, 2, 15));

        verifyNoMoreInteractions(payoutService);
    }

    @Test
    @DisplayName("POST /api/v1/payouts/bulk - skips invalid lines and ingests only valid ones")
    void ingest_skipsInvalidRows_andStillProcessesValidOnes() throws Exception {
        // This CSV includes a variety of malformed rows:
        // - fewer than 3 columns
        // - empty mandatory fields
        // - non-numeric amount
        // - amount exceeding DECIMAL(19,2)
        // - invalid date
        // - blank line
        // It ends with one valid row that should pass through.
        String csv = String.join("\n",
                "transactionId,payoutAmount,payoutDate",
                "BAD1,100.00",                              // missing payoutDate -> cols < 3
                "BAD2,,2023-01-01",                         // empty payoutAmount
                "BAD3,abc,2023-01-02",                      // invalid amount
                "BAD4,100000000000000000.00,2023-01-03",    // beyond 99999999999999999.99 -> should be skipped
                "BAD5,10.00,2020-13-01",                    // invalid date (month 13)
                "",                                         // blank line
                "OK1,99.99,2023-12-31"                      // valid row
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "payouts.csv", "text/csv", csv.getBytes()
        );

        mockMvc.perform(multipart("/api/v1/payouts/bulk").file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().string("CSV ingest complete"));

        // Service should be called once with only the valid row.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Payout>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(payoutService, times(1)).ingestChunk(captor.capture());
        List<Payout> ingested = captor.getValue();

        assertThat(ingested).hasSize(1);
        Payout p = ingested.get(0);
        assertThat(p.getTransactionId()).isEqualTo("OK1");
        assertThat(p.getPayoutAmount()).isEqualByComparingTo("99.99");
        assertThat(p.getPayoutDate()).isEqualTo(LocalDate.of(2023, 12, 31));

        verifyNoMoreInteractions(payoutService);
    }

    @Test
    @DisplayName("POST /api/v1/payouts/bulk - honors 500-row chunking when ingesting")
    void ingest_callsServiceInChunksOf500() throws Exception {
        // The controller flushes the buffer when it reaches 500 rows.
        // Build 1001 valid rows to force 3 service calls: 500, 500, and 1.
        String csv = buildCsvWithValidRows(1001);

        MockMultipartFile file = new MockMultipartFile(
                "file", "payouts.csv", "text/csv", csv.getBytes()
        );

        // IMPORTANT for this test:
        // The controller passes a mutable List<Payout> 'buffer' to the service, then calls buffer.clear().
        // If we use ArgumentCaptor on that same instance, all captured references will reflect the final state.
        // To assert real batch sizes, snapshot each call's argument at invocation time.
        List<List<Payout>> snapshots = new ArrayList<>();
        doAnswer(invocation -> {
            List<Payout> batch = invocation.getArgument(0);
            // Make a defensive copy so later 'clear()' in controller won't affect our assertions.
            snapshots.add(new ArrayList<>(batch));
            return null;
        }).when(payoutService).ingestChunk(anyList());

        mockMvc.perform(multipart("/api/v1/payouts/bulk").file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().string("CSV ingest complete"));

        // We expect 3 invocations total.
        verify(payoutService, times(3)).ingestChunk(anyList());

        // Now assert sizes from our defensive snapshots.
        assertThat(snapshots).hasSize(3);
        assertThat(snapshots.get(0)).hasSize(500);
        assertThat(snapshots.get(1)).hasSize(500);
        assertThat(snapshots.get(2)).hasSize(1);

        // Spot-check first record to ensure correct parsing and mapping.
        Payout first = snapshots.get(0).get(0);
        assertThat(first.getTransactionId()).isEqualTo("TXN-1");
        assertThat(first.getPayoutAmount()).isEqualByComparingTo("1.00");
        assertThat(first.getPayoutDate()).isEqualTo(LocalDate.of(2024, 1, 1));

        // Spot-check very last record to confirm final flush occurred.
        Payout last = snapshots.get(2).get(0);
        assertThat(last.getTransactionId()).isEqualTo("TXN-1001");
        assertThat(last.getPayoutAmount()).isEqualByComparingTo("1001.00");
        assertThat(last.getPayoutDate()).isEqualTo(LocalDate.of(2024, 1, 1));

        verifyNoMoreInteractions(payoutService);
    }

    @Test
    @DisplayName("GET /api/v1/payouts/ - returns all payouts as JSON array")
    void getPayouts_returnsList() throws Exception {
        // Prepare two sample payouts that the mocked service will return.
        List<Payout> mockList = new ArrayList<>();
        Payout p1 = new Payout();
        p1.setTransactionId("A1");
        p1.setPayoutAmount(new BigDecimal("10.50"));
        p1.setPayoutDate(LocalDate.of(2024, 3, 1));
        mockList.add(p1);

        Payout p2 = new Payout();
        p2.setTransactionId("A2");
        p2.setPayoutAmount(new BigDecimal("20.00"));
        p2.setPayoutDate(LocalDate.of(2024, 3, 2));
        mockList.add(p2);

        when(payoutService.findAllPayouts()).thenReturn(mockList);

        // Assert JSON shape and values. Spring serializes BigDecimal to JSON numbers by default.
        mockMvc.perform(get("/api/v1/payouts/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].transactionId", is("A1")))
                .andExpect(jsonPath("$[0].payoutAmount", is(10.50)))
                .andExpect(jsonPath("$[0].payoutDate", is("2024-03-01")))
                .andExpect(jsonPath("$[1].transactionId", is("A2")))
                .andExpect(jsonPath("$[1].payoutAmount", is(20.00)))
                .andExpect(jsonPath("$[1].payoutDate", is("2024-03-02")));

        verify(payoutService, times(1)).findAllPayouts();
        verifyNoMoreInteractions(payoutService);
    }

    @Test
    @DisplayName("GET /api/v1/payouts/{id} - returns a payout when Optional is present")
    void getPayoutById_returnsPresent() throws Exception {
        // Prepare a matching payout for the requested id.
        Payout p = new Payout();
        p.setTransactionId("ABC-123");
        p.setPayoutAmount(new BigDecimal("33.33"));
        p.setPayoutDate(LocalDate.of(2024, 2, 29));

        when(payoutService.findPayoutById("ABC-123")).thenReturn(Optional.of(p));

        // Controller returns Optional<Payout> directly, which Spring serializes as the contained object.
        mockMvc.perform(get("/api/v1/payouts/{id}", "ABC-123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactionId", is("ABC-123")))
                .andExpect(jsonPath("$.payoutAmount", is(33.33)))
                .andExpect(jsonPath("$.payoutDate", is("2024-02-29")));

        verify(payoutService, times(1)).findPayoutById("ABC-123");
        verifyNoMoreInteractions(payoutService);
    }

    @Test
    @DisplayName("GET /api/v1/payouts/{id} - returns 200 with JSON null when Optional is empty")
    void getPayoutById_returnsJsonNullWhenNotFound() throws Exception {
        // Returning Optional.empty() from controller results in a JSON "null" body with application/json content type.
        when(payoutService.findPayoutById("NOT-THERE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/payouts/{id}", "NOT-THERE"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // Spring writes literal 'null' for Optional.empty()
                .andExpect(content().string("null"));

        verify(payoutService, times(1)).findPayoutById("NOT-THERE");
        verifyNoMoreInteractions(payoutService);
    }

    /**
     * Builds a valid CSV with N rows.
     * - transactionId: "TXN-i"
     * - payoutAmount: "i.00"
     * - payoutDate: "2024-01-01"
     *
     * This is used to test the controller's chunking behavior.
     */
    private String buildCsvWithValidRows(int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("transactionId,payoutAmount,payoutDate\n");
        for (int i = 1; i <= n; i++) {
            sb.append("TXN-").append(i).append(",")
                    .append(i).append(".00,")
                    .append("2024-01-01")
                    .append("\n");
        }
        return sb.toString();
    }
}