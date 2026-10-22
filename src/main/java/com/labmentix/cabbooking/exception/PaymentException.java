package com.labmentix.cabbooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when payment processing, confirmation, or refund encounters an error.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
