package com.nsbm.authservice.exception;

public class StaffAlreadyExistsException extends RuntimeException {
    public StaffAlreadyExistsException(String message) {
        super(message);
    }
}