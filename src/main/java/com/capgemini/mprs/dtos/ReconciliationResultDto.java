package com.capgemini.mprs.dtos;

import com.capgemini.mprs.entities.ReconciliationException;
import com.capgemini.mprs.entities.ReconciliationSummary;

import java.util.List;

public class ReconciliationResultDto {

    private final ReconciliationSummary summary;
    private final List<ReconciliationException> exceptions;

    public ReconciliationResultDto(ReconciliationSummary summary, List<ReconciliationException> exceptions) {
        this.summary = summary;
        this.exceptions = exceptions;
    }

    public ReconciliationSummary summary() {
        return summary;
    }

    public List<ReconciliationException> exceptions() {
        return exceptions;
    }

}
