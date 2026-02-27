package com.capgemini.mprs.transaction;

import java.util.List;

public interface TransactionBatchOps {

    void saveInBatches(List<Transaction> items);

}
