package com.capgemini.mprs;

import com.capgemini.mprs.dtos.RunRequestDto;
import com.capgemini.mprs.entities.ReconciliationException;
import com.capgemini.mprs.entities.ReconciliationJob;
import com.capgemini.mprs.entities.ReconciliationSummary;
import com.capgemini.mprs.services.ReconciliationExceptionService;
import com.capgemini.mprs.services.ReconciliationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class ReconciliationControllerTest {

    @MockitoBean
    private ReconciliationService reconciliationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /run triggers reconciliation and returns 202")
    void testRun() throws Exception{

        String json = """
        {
            "startDate": "2026-03-10",
            "endDate": "2026-03-10"
        }
        """;

        mockMvc.perform(post("/api/v1/reconciliation/run")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isAccepted());

        verify(reconciliationService).triggerAsync(any(RunRequestDto.class));

    }

    @Test
    @DisplayName("GET /{jobId} triggers getting a job by ID and returns a reconciliationJobDto")
    void testGetJobById() throws Exception{

        ReconciliationJob job = new ReconciliationJob();
        job.setId(123L);
        job.setStatus(ReconciliationJob.JobStatus.SUCCEEDED);

        // Mock the service call
        when(reconciliationService.findStatusById(123L)).thenReturn(job);

        // Act + Assert
        mockMvc.perform(get("/api/v1/reconciliation/123")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.status").value(ReconciliationJob.JobStatus.SUCCEEDED.name()));

        // Verify service interaction
        verify(reconciliationService).findStatusById(123L);


    }

    @Test
    @DisplayName("GET /{jobId}/summary triggers getting a job summary by ID and returns a ReconciliationSummary")
    void testGetJobSummaryById() throws Exception{

        long jobId = 123L;

        when(reconciliationService.findStatusById(jobId)).thenReturn(new ReconciliationJob());

        ReconciliationSummary summary = new ReconciliationSummary();

        when(reconciliationService.findReconciliationSummaryByJobId(jobId))
                .thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/v1/reconciliation/{jobId}/summary", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());

        verify(reconciliationService).findStatusById(jobId);
        verify(reconciliationService).findReconciliationSummaryByJobId(jobId);

    }

    @Test
    @DisplayName("Get /{jobId}/exceptions triggers getting job exceptions by job ID and returns a ReconciliationException")
    void testGetJobExceptionsById() throws Exception{
        Long jobId = 123L;

        when(reconciliationService.findStatusById(jobId)).thenReturn(new ReconciliationJob());

        List<ReconciliationException> exceptions = new ArrayList<>();

        when(reconciliationService.findReconciliationExceptionsByJobId(jobId)).thenReturn(exceptions);

        mockMvc.perform(get("/api/v1/reconciliation/{jobId}/exceptions", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.length()").value(0));

        verify(reconciliationService).findStatusById(jobId);
        verify(reconciliationService).findReconciliationExceptionsByJobId(jobId);

    }
}
