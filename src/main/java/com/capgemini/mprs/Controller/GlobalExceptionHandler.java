package com.capgemini.mprs.Controller;

import com.capgemini.mprs.Entity.ExceptionEntity;
import com.capgemini.mprs.Repository.ExceptionEntityRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @Autowired
    private ExceptionEntityRepository repository;
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ExceptionEntity> handleIOException(IOException ex, HttpServletRequest request) {
        logger.error(
                "Exception occurred at endpoint {}: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        ExceptionEntity error = new ExceptionEntity
                (HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "File Processing Error",
                        ex.getMessage(),
                        request.getRequestURI());
        repository.save(error);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ExceptionEntity> handleMultipartException(MultipartException ex, HttpServletRequest request) {
        logger.error(
                "Exception occurred at endpoint {}: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        ExceptionEntity error = new ExceptionEntity
                (HttpStatus.BAD_REQUEST.value(),
                        "File Upload Error",
                        ex.getMessage(),
                        request.getRequestURI());
        repository.save(error);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionEntity> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        logger.error(
                "Exception occurred at endpoint {}: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        ExceptionEntity error = new ExceptionEntity
                (HttpStatus.BAD_REQUEST.value(),
                        "Invalid request parameter",
                        ex.getMessage(),
                        request.getRequestURI());
        repository.save(error);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionEntity> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        logger.error(
                "Exception occurred at endpoint {}: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        ExceptionEntity error = new ExceptionEntity
                (HttpStatus.BAD_REQUEST.value(),
                        "Invalid argument",
                        ex.getMessage(),
                        request.getRequestURI());
        repository.save(error);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionEntity> handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error(
                "Exception occurred at endpoint {}: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        ExceptionEntity error = new ExceptionEntity
                (HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "An unexpected server error occurred",
                        ex.getMessage(),
                        request.getRequestURI());
        repository.save(error);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}