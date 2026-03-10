package com.capgemini.mprs.Dto;

import com.capgemini.mprs.Entity.ExceptionEntity;
import com.capgemini.mprs.Entity.Payout;
import com.capgemini.mprs.Entity.Transaction;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class ProcessingResult {
    private ReconciliationSummary summary;
    private ExceptionEntity error;
    private Transaction transaction;
    private Payout payout;
    public boolean isError()
    {
        return error != null;
    }

}
