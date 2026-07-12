package com.foodmarket.notification.exception;

/** Lanzada cuando una notificación solicitada no existe. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
