package com.capgemini.mprs.controllers;

import com.capgemini.mprs.custom_exceptions.TransactionNotFoundException;
import com.capgemini.mprs.entities.Transaction;
import com.capgemini.mprs.services.TransactionService;
import jakarta.validation.Valid;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    @PostMapping(value = "/bulk", consumes = "multipart/form-data")
    public ResponseEntity<String> ingest(@RequestParam MultipartFile file) throws Exception {
        final int bufferSize = 500;

        List<Transaction> buffer = new ArrayList<>(bufferSize);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNum = 0;
            int numOfRowsSkipped = 0;
            ArrayList<String> rowsSkipped = new ArrayList<>();

            // Skip CSV header
            br.readLine();
            lineNum++;

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                lineNum++;

                String[] cols = line.split(",");

                if(cols.length < 5){
                    log.warn("IllegalArgumentException - Entry skipped because it has one or more missing fields: " + line);
                    numOfRowsSkipped++;
                    rowsSkipped.add(line);
                    continue;
                }

                if(cols[0].isEmpty()||cols[1].isEmpty()||cols[2].isEmpty()||cols[3].isEmpty()||cols[4].isEmpty()){
                    log.warn("IllegalArgumentException - Entry skipped because it has one or more empty fields: " + line);
                    numOfRowsSkipped++;
                    rowsSkipped.add(line);
                    continue;
                }


                Transaction t = new Transaction();
                t.setTransactionId(cols[0]);

                try {
                    BigDecimal am = new BigDecimal(cols[1]);
                    if (am.abs().compareTo(new BigDecimal("99999999999999999.99")) > 0) {
                        log.warn("IllegalArgumentException - Skipping line {}: amount '{}' exceeds DECIMAL(19,2). Line: {}", lineNum, cols[1], line);
                        numOfRowsSkipped++;
                        continue;
                    }

                    t.setAmount(am);
                } catch (NumberFormatException nfe) {
                    numOfRowsSkipped++;
                    rowsSkipped.add(line);
                    log.warn("NumberFormatException - Skipping line {}: invalid amount '{}'. Line: {}", lineNum, cols[1], line);
                    continue;
                }

                try{
                    t.setStatus(Transaction.Status.valueOf(cols[2].toUpperCase()));
                }catch (IllegalArgumentException iae){
                    numOfRowsSkipped++;
                    rowsSkipped.add(line);
                    log.warn("IllegalArgumentException - Skipping line {}: invalid status '{}'. Line: {}", lineNum, cols[2], line);
                    continue;
                }

                try {
                    LocalDate date;
                    try {
                        date = LocalDate.parse(cols[3]); // yyyy-MM-dd
                    } catch (Exception e1) {
                        date = LocalDate.parse(cols[3], DateTimeFormatter.ofPattern("dd-MM-yyyy")); // 28-01-2026
                    }
                    t.setSettlementDate(date);
                } catch (DateTimeParseException e) {
                    log.warn("DateTimeParseException - Skipping line {}: invalid date '{}'. Line: {}", lineNum, cols[3], line);
                    numOfRowsSkipped++;
                    rowsSkipped.add(line);
                    continue;
                }

                String merchantRaw = cols[4].trim();
                try{
                    if(merchantRaw.length() > 64){
                        log.warn("IllegalArgumentException - Skipping line {}: invalid merchantId '{}'. Line: {}", lineNum, cols[4], line);
                        numOfRowsSkipped++;
                        rowsSkipped.add(line);
                        continue;
                    }
                    t.setMerchantId(cols[4]);
                }catch(Exception e){
                    log.warn("IllegalArgumentException - Skipping line {}: failed to set merchantId '{}'. Line: {}", lineNum, merchantRaw, line, e);
                    numOfRowsSkipped++;
                    rowsSkipped.add(line);
                    continue;

                }

                buffer.add(t);

                if (buffer.size() >= bufferSize) {
                    transactionService.ingestChunk(buffer);
                    buffer.clear();
                }
            }

            if (!buffer.isEmpty()) {
                transactionService.ingestChunk(buffer);
            }
            log.info("Total Rows Skipped: " + numOfRowsSkipped + " rows.");
            for(String s: rowsSkipped){
                log.info("Skipped Row: " + s);
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

        Optional<Transaction> t = transactionService.findTransactionById(id);
        try {
            if(t.isEmpty()){
                throw new TransactionNotFoundException("Transaction Not Found For Given Id.");
            }
            return t;
        }catch (IllegalArgumentException e){
            throw new IllegalArgumentException();
        }
    }

}
