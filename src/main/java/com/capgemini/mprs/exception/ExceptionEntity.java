package com.capgemini.mprs.exception;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "exceptions")
@Data
@NoArgsConstructor
public class ExceptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exception_id", unique = true,updatable = false, nullable = false)
    private Long exception_id;
    private Instant timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;

    public ExceptionEntity(Integer status, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

}
