package com.geek.booking.exception;

public class DuplicateBookingException extends BusinessException {
    public DuplicateBookingException(String message) {
        super(message);
    }
}