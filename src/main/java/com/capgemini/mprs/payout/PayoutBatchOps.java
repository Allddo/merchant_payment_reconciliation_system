package com.capgemini.mprs.payout;


import java.util.List;

public interface PayoutBatchOps {

    void saveInBatches(List<Payout> items);

}
