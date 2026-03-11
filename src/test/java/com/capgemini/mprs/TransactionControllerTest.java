package com.capgemini.mprs;

import com.capgemini.mprs.entities.Transaction;
import com.capgemini.mprs.services.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @MockitoBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    private Transaction tx(String id, String amount, Transaction.Status status, String dateIso, String merchant) {
        Transaction t = new Transaction();
        t.setTransactionId(id);
        t.setAmount(new BigDecimal(amount));
        t.setStatus(status);
        t.setSettlementDate(LocalDate.parse(dateIso)); // yyyy-MM-dd
        t.setMerchantId(merchant);
        return t;
    }

    @Nested
    class BulkIngestTests {

        @Test
        @DisplayName("POST /api/v1/transactions/bulk — ingests valid rows and returns 200 OK")
        void ingestBulk_success() throws Exception {
            String csv = String.join("\n",
                    "transactionId,amount,status,settlementDate,merchantId", // header (skipped)
                    "T1,123.45,SETTLED,2026-01-28,M001",
                    // dd-MM-yyyy format should be parsed too:
                    "T2,10.00,AUTHORIZED,28-01-2026,M002"
            );

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "tx.csv",
                    "text/csv",
                    csv.getBytes()
            );

            // no need to stub service.return; just verify it's called
            ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);

            mockMvc.perform(
                            multipart("/api/v1/transactions/bulk")
                                    .file(file)
                                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("CSV ingest complete")));

            // Controller batches and flushes once at the end for small uploads
            verify(transactionService, times(1)).ingestChunk(captor.capture());
            List<Transaction> sent = captor.getValue();
            assertThat(sent).hasSize(2);

            // Validate parsed fields
            Transaction a = sent.get(0);
            assertThat(a.getTransactionId()).isEqualTo("T1");
            assertThat(a.getAmount()).isEqualByComparingTo("123.45");
            assertThat(a.getStatus()).isEqualTo(Transaction.Status.SETTLED);
            assertThat(a.getSettlementDate()).isEqualTo(LocalDate.parse("2026-01-28"));
            assertThat(a.getMerchantId()).isEqualTo("M001");

            Transaction b = sent.get(1);
            assertThat(b.getTransactionId()).isEqualTo("T2");
            assertThat(b.getAmount()).isEqualByComparingTo("10.00");
            assertThat(b.getStatus()).isEqualTo(Transaction.Status.AUTHORIZED);
            assertThat(b.getSettlementDate()).isEqualTo(LocalDate.parse("2026-01-28"));
            assertThat(b.getMerchantId()).isEqualTo("M002");
        }

        @Test
        @DisplayName("POST /api/v1/transactions/bulk — skips invalid rows and only ingests valid ones")
        void ingestBulk_skipsInvalidRows() throws Exception {
            String longMerchant = "X".repeat(65); // > 64 chars → should be skipped

            String csv = String.join("\n",
                    "transactionId,amount,status,settlementDate,merchantId",
                    // valid
                    "T1,5.00,SETTLED,2026-02-01,MERCH_OK",
                    // invalid amount (NumberFormatException)
                    "T2,abc,SETTLED,2026-02-01,MERCH_OK",
                    // invalid status
                    "T3,1.00,NOT_A_STATUS,2026-02-01,MERCH_OK",
                    // invalid date
                    "T4,2.50,SETTLED,2026-99-99,MERCH_OK",
                    // empty field (merchant)
                    "T5,2.50,SETTLED,2026-02-01,",
                    // missing fields (< 5 cols)
                    "T6,2.50,SETTLED,2026-02-01",
                    // merchant too long
                    "T7,9.99,SETTLED,2026-02-01," + longMerchant
            );

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "tx_bad.csv",
                    "text/csv",
                    csv.getBytes()
            );

            ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);

            mockMvc.perform(
                            multipart("/api/v1/transactions/bulk")
                                    .file(file)
                                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("CSV ingest complete")));

            verify(transactionService, times(1)).ingestChunk(captor.capture());
            List<Transaction> sent = captor.getValue();

            // Only T1 is valid in that set
            assertThat(sent).extracting(Transaction::getTransactionId).containsExactly("T1");
            assertThat(sent.get(0).getAmount()).isEqualByComparingTo("5.00");
            assertThat(sent.get(0).getStatus()).isEqualTo(Transaction.Status.SETTLED);
            assertThat(sent.get(0).getSettlementDate()).isEqualTo(LocalDate.parse("2026-02-01"));
            assertThat(sent.get(0).getMerchantId()).isEqualTo("MERCH_OK");
        }
    }

    @Test
    @DisplayName("GET /api/v1/transactions/ — returns list of transactions")
    void getAllTransactions_returnsList() throws Exception {
        List<Transaction> sample = List.of(
                tx("A1", "1.23", Transaction.Status.SETTLED, "2026-02-05", "M1"),
                tx("A2", "9.99", Transaction.Status.AUTHORIZED, "2026-02-06", "M2")
        );
        when(transactionService.findAllTransactions()).thenReturn(sample);

        mockMvc.perform(get("/api/v1/transactions/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].transactionId").value("A1"))
                .andExpect(jsonPath("$[0].amount").value(1.23))
                .andExpect(jsonPath("$[0].status").value("SETTLED"))
                .andExpect(jsonPath("$[0].settlementDate").value("2026-02-05"))
                .andExpect(jsonPath("$[0].merchantId").value("M1"))
                .andExpect(jsonPath("$[1].transactionId").value("A2"))
                .andExpect(jsonPath("$[1].amount").value(9.99))
                .andExpect(jsonPath("$[1].status").value("AUTHORIZED"))
                .andExpect(jsonPath("$[1].settlementDate").value("2026-02-06"))
                .andExpect(jsonPath("$[1].merchantId").value("M2"));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{id} — returns the transaction when found")
    void getById_found_returns200() throws Exception {
        Transaction t = tx("Z9", "100.00", Transaction.Status.SETTLED, "2026-02-07", "MERCHZ");
        when(transactionService.findTransactionById("Z9")).thenReturn(Optional.of(t));

        mockMvc.perform(get("/api/v1/transactions/{id}", "Z9"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactionId").value("Z9"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.status").value("SETTLED"))
                .andExpect(jsonPath("$.settlementDate").value("2026-02-07"))
                .andExpect(jsonPath("$.merchantId").value("MERCHZ"));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{id} — returns 404 when not found")
    void getById_notFound_returns404() throws Exception {
        when(transactionService.findTransactionById("NOPE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/transactions/{id}", "NOPE"))
                .andExpect(status().isNotFound()); // Requires @ResponseStatus(NOT_FOUND) on TransactionNotFoundException or a @ControllerAdvice mapping
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{id} — returns 400 on IllegalArgumentException (e.g., malformed id)")
    void getById_illegalArgument_returns400() throws Exception {
        when(transactionService.findTransactionById("BAD!")).thenThrow(new IllegalArgumentException("bad id"));

        mockMvc.perform(get("/api/v1/transactions/{id}", "BAD!"))
                .andExpect(status().isBadRequest()); // Requires @ControllerAdvice mapping IllegalArgumentException -> 400
    }
}