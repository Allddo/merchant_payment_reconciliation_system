package com.capgemini.mprs.reconciliation;

import java.time.Instant;
import java.time.LocalDate;

public class JobDto {

    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;   // RUNNING, SUCCEEDED, FAILED
    private Instant createdAt;


    public static JobDto from(ReconciliationJob job) {
        JobDto dto = new JobDto();
        dto.id = job.getId();
        dto.status = job.getStatus().name();
        dto.startDate = job.getStartDate();
        dto.endDate = job.getEndDate();
        dto.createdAt = job.getCreatedAt();
        return dto;
    }


}