package com.capgemini.mprs.reconciliation;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RunRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}
