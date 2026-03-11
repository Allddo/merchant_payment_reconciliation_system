package com.capgemini.mprs.dtos;

import com.capgemini.mprs.entities.ReconciliationJob;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
public class JobDto {

    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;   // RUNNING, SUCCEEDED, FAILED
    private Instant createdAt;
    private Instant updatedAt;


    public static JobDto from(ReconciliationJob job) {
        JobDto dto = new JobDto();
        dto.id = job.getId();
        dto.status = job.getStatus().name();
        dto.startDate = job.getStartDate();
        dto.endDate = job.getEndDate();
        dto.createdAt = job.getCreatedAt();
        dto.updatedAt = job.getUpdatedAt();
        return dto;
    }


}