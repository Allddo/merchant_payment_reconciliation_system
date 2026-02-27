package com.capgemini.mprs.payout;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;

    @Transactional
    public void ingestChunk(List<Payout> chunk){
        payoutRepository.saveInBatches(chunk);
    }

    public List<Payout> findAllPayouts(){
        return payoutRepository.findAll();
    }

    public Optional<Payout> findPayoutById(String id){
        return payoutRepository.findById(id);
    }

}
