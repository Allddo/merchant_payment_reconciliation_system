package com.capgemini.mprs.custom_exceptions;

public class DuplicateJobExceptionCust extends RuntimeException {
    public DuplicateJobExceptionCust(String message) {
        super(message);
    }
}
