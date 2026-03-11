package com.capgemini.mprs.global_exception_handler;

import com.capgemini.mprs.custom_exceptions.DuplicateJobExceptionCust;
import com.capgemini.mprs.custom_exceptions.JobNotFoundException;
import com.capgemini.mprs.dtos.ApiErrorDto;
import org.aspectj.weaver.ast.Not;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 400 — Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDto> handleBadRequest(
            IllegalArgumentException ex, WebRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    //400 - NumberFormatException
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiErrorDto> handleBadRequest(NumberFormatException ex, WebRequest request){

        ApiErrorDto error = new ApiErrorDto(
                HttpStatus.BAD_REQUEST.value(),
                "NUMBER_FORMAT_ERROR",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


    // 404 — Not Found (Transaction)
    @ExceptionHandler(com.capgemini.mprs.custom_exceptions.TransactionNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleTransactionNotFound(
            com.capgemini.mprs.custom_exceptions.TransactionNotFoundException ex,
            WebRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

        @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleNotFound(
            JobNotFoundException ex, WebRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 409 — Conflict
    @ExceptionHandler(DuplicateJobExceptionCust.class)
    public ResponseEntity<ApiErrorDto> handleConflict(
            DuplicateJobExceptionCust ex, WebRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 500 — Generic server error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleInternal(
            Exception ex, WebRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }
}