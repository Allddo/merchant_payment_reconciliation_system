package com.capgemini.mprs.transaction;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;


    @PostMapping(value = "/bulk", consumes = "multipart/form-data")
    public ResponseEntity<String> ingest(@RequestParam MultipartFile file) throws Exception {
        final int bufferSize = 500;

        List<Transaction> buffer = new ArrayList<>(bufferSize);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;

            // Skip CSV header
            br.readLine();

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] cols = line.split(",");

                Transaction t = new Transaction();
                t.setTransactionId(cols[0]);
                t.setAmount(new BigDecimal(cols[1]));
                t.setStatus(Transaction.Status.valueOf(cols[2]));
                t.setSettlementDate(LocalDate.parse(cols[3]));
                t.setMerchantId(cols[4]);

                buffer.add(t);

                if (buffer.size() >= bufferSize) {
                    transactionService.ingestChunk(buffer);
                    buffer.clear();
                }
            }

            if (!buffer.isEmpty()) {
                transactionService.ingestChunk(buffer);
            }
        }

        return ResponseEntity.ok("CSV ingest complete");
    }

    @GetMapping("/")
    public List<Transaction> getTransactions(){
        return transactionService.findAllTransactions();
    }

    @GetMapping("/{id}")
    public Optional<Transaction> getTransactionById(@PathVariable String id){
        return transactionService.findTransactionById(id);
    }

}
