package com.capgemini.mprs.controllers;

import com.capgemini.mprs.entities.Payout;
import com.capgemini.mprs.services.PayoutService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payouts")
public class PayoutController {


    private static final Logger log = LoggerFactory.getLogger(PayoutController.class);
    private static final BigDecimal MAX_DECIMAL_19_2 = new BigDecimal("99999999999999999.99");

    private final PayoutService payoutService;

    @PostMapping(value = "/bulk", consumes = "multipart/form-data")
    public ResponseEntity<String> ingest(@RequestParam MultipartFile file) throws Exception {
        final int bufferSize = 500;

        List<Payout> buffer = new ArrayList<>(bufferSize);

        try(BufferedReader br= new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNum = 0;
            int numOfRowsSkipped = 0;
            List<String> rowsSkipped = new ArrayList<>();

            // Skip CSV header
            br.readLine();
            lineNum++;

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                lineNum++;

                String[] cols = line.split(",");

                if (cols.length < 3) {
                    log.warn("IllegalArgumentException - Entry skipped because it has one or more missing fields: {}", line);
                    numOfRowsSkipped++;
                    rowsSkipped.add(line);
                    continue;
                }

                if (cols[0].isEmpty() || cols[1].isEmpty() || cols[2].isEmpty()) {
                    log.warn("IllegalArgumentException - Entry skipped because it has one or more empty fields: {}", line);
                    numOfRowsSkipped++;
                    rowsSkipped.add(line);
                    continue;
                }

                Payout p = new Payout();
                p.setTransactionId(cols[0]);

                // Trim columns before further validation
                String col0 = cols[0].trim(); // transactionId
                String col1 = cols[1].trim(); // payoutAmount
                String col2 = cols[2].trim(); // payoutDate

                try {
                    BigDecimal amount = new BigDecimal(col1);
                    if (amount.abs().compareTo(MAX_DECIMAL_19_2) > 0) {
                        log.warn("IllegalArgumentException - Skipping line {}: payoutAmount '{}' exceeds DECIMAL(19,2). Line: {}", lineNum, col1, line);
                        numOfRowsSkipped++;
                        rowsSkipped.add(line);
                        continue;
                    }
                    p.setPayoutAmount(amount);
                } catch (NumberFormatException nfe) {
                    numOfRowsSkipped++;
                    rowsSkipped.add(line);
                    log.warn("NumberFormatException - Skipping line {}: invalid payoutAmount '{}'. Line: {}", lineNum, col1, line);
                    continue;
                }

                try {
                    LocalDate payoutDate;
                    try {
                        payoutDate = LocalDate.parse(col2); // yyyy-MM-dd
                    } catch (Exception e1) {
                        payoutDate = LocalDate.parse(col2, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    }
                    p.setPayoutDate(payoutDate);
                } catch (DateTimeParseException dte) {
                    log.warn("DateTimeParseException - Skipping line {}: invalid payoutDate '{}'. Line: {}", lineNum, col2, line);
                    numOfRowsSkipped++;
                    rowsSkipped.add(line);
                    continue;
                }

                buffer.add(p);

                if (buffer.size() >= bufferSize) {
                    payoutService.ingestChunk(buffer);
                    buffer.clear();
                }
            }

            if (!buffer.isEmpty()) {
                payoutService.ingestChunk(buffer);
            }

            log.info("Total Rows Skipped: {} rows.", numOfRowsSkipped);
            for (String s : rowsSkipped) {
                log.info("Skipped Row: {}", s);
            }
        }
        return ResponseEntity.ok("CSV ingest complete");
    }

    @GetMapping("/")
    public List<Payout> getPayouts(){
        return payoutService.findAllPayouts();
    }

    @GetMapping("/{id}")
    public Optional<Payout> getPayoutById(@PathVariable String id){
        return payoutService.findPayoutById(id);
    }

}
