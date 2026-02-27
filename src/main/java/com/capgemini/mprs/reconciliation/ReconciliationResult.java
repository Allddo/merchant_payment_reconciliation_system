package com.capgemini.mprs.reconciliation;

import com.capgemini.mprs.exception.ReconciliationException;

import java.util.List;

public class ReconciliationResult {

    private final ReconciliationSummary summary;
    private final List<ReconciliationException> exceptions;

    public ReconciliationResult(ReconciliationSummary summary, List<ReconciliationException> exceptions) {
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
