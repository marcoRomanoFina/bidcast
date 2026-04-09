package com.bidcast.venue_service.exception;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ErrorResponse {
    
    private LocalDateTime timestamp = LocalDateTime.now();
    private int status;
    private String error;
    private String message;

    public ErrorResponse(int status, String error, String message){
        this.status = status;
        this.error = error;
        this.message = message;
    }
}
