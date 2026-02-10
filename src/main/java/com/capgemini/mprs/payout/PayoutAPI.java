package com.capgemini.mprs.payout;

import com.capgemini.mprs.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payouts")
public class PayoutAPI {
    private final PayoutService service;

    @Autowired
    public PayoutAPI(PayoutService service)
    {
        this.service = service;
    }

    @GetMapping("/{id}")
    public Payout getPayout(@PathVariable Integer id) {
        return service.get(id);
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> addPayout(@RequestBody List<Payout> payouts)
    {
        service.createPayouts(payouts);
        return ResponseEntity.status(HttpStatus.CREATED).body(payouts);
    }
}
