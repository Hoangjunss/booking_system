package com.geek.booking.exception;

public class InsufficientInventoryException extends BusinessException {
    public InsufficientInventoryException(String message) {
        super(message);
    }
}