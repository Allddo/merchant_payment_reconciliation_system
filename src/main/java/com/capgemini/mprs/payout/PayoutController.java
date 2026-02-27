package com.capgemini.mprs.payout;

import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
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
@RequestMapping("/api/v1/payouts")
public class PayoutController {

    private final PayoutService payoutService;

    @PostMapping(value = "/bulk", consumes = "multipart/form-data")
    public ResponseEntity<String> ingest(@RequestParam MultipartFile file) throws Exception{
        final int bufferSize = 500;

        List<Payout> buffer = new ArrayList<>(bufferSize);

        try(BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))){
            String line;

            br.readLine();

            while((line = br.readLine()) != null){

                if(line.isBlank()) continue;

                String[] cols = line.split(",");

                Payout p = new Payout();
                p.setTransactionId(cols[0]);
                p.setPayoutAmount(new BigDecimal(cols[1]));
                p.setPayoutDate(LocalDate.parse(cols[2]));

                buffer.add(p);

                if(buffer.size() >= bufferSize){
                    payoutService.ingestChunk(buffer);
                    buffer.clear();
                }
            }

            if(!buffer.isEmpty()){
                payoutService.ingestChunk(buffer);
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
