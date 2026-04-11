package com.bidcast.session_service.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SessionExceptionHandler {

    @ExceptionHandler(OpenSessionAlreadyExistsException.class)
    public ProblemDetail handleOpenSessionAlreadyExists(OpenSessionAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Open session already exists");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(SessionNotificationException.class)
    public ProblemDetail handleSessionNotification(SessionNotificationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle("Selection notification failed");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ProblemDetail handleSessionNotFound(SessionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Session not found");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(SessionDeviceNotFoundException.class)
    public ProblemDetail handleSessionDeviceNotFound(SessionDeviceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Session device not found");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
