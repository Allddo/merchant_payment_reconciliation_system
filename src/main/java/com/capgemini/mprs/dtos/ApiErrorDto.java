package com.capgemini.mprs.dtos;

import java.time.Instant;

public class ApiErrorDto {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public ApiErrorDto(int status, String error, String message, String path) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public String getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }

}