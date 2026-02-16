package com.capgemini.mprs.payout;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;

    public List<Payout> findAllPayouts(){
        return payoutRepository.findAll();
    }

    public Optional<Payout> findPayoutById(String id){
        return payoutRepository.findById(id);
    }

}
