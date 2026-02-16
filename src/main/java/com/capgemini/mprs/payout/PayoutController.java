package com.capgemini.mprs.payout;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payouts")
public class PayoutController {

    private final PayoutService payoutService;

    @GetMapping("/")
    public List<Payout> getPayouts(){
        return payoutService.findAllPayouts();
    }

    @GetMapping("/{id}")
    public Optional<Payout> getPayoutById(@PathVariable String id){
        return payoutService.findPayoutById(id);
    }

}
